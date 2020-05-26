import groovy.ui.SystemOutputInterceptor

/**
 * Simple wrapper script for running arbitrary shell commands, killing them on timeout and
 * performing custom logic just before timeout (Something that Jenkins DSL can't do)
 *
 * Running in Jenkins (example):
 *  try {
 *      sh "groovy this-script.groovy -t 300 -c './gradlew some tasks' -f thread-dumps.txt"
 *  } finally {
 *      archiveArtifacts artifacts: 'thread-dumps.txt'
 *  }
 *
 *
 *  Parameters:
 *   Timeout after 5 minutes (Jenkins equivalent: timeout(5) {}):
 *      -t 300
 *   Timeout after 5 minutes of inactivity (Jenkins equivalent: timeout(time: 5, activity: true) {})
 *      -t 300 -i
 *   Run something other than jps thread dump on timeout:
 *      -t 300 -k "some command"
 *
 * If command takes more than 5 minutes, this script will perform -k and abort the process
 * Omitting -k causes the script to run the default "stacktrace on all java processes" handler
 * Script exits with code 100 on timeout.
 */

// Parse command line
def cli = new CliBuilder(usage: '-c command -t timeout [other options]')
cli.with {
    t longOpt: 'timeout', args: 1, 'Timeout in seconds (either absolute or inactivity)'
    i longOpt: 'inactivity', argName: 'inactivity', 'Timeout on inactivity after timeout seconds'
    c longOpt: 'command', args: 1, argName: 'command', 'Command to execute'
    k longOpt: 'before-kill-command', args: 1, argName: 'command', 'Command to execute before killing the executing command.  Defaults to JPS analysis of all java processes'
    f longOpt: 'before-kill-output-to-file', args: 1, argName: 'filename', 'Redirect output of before-kill-command to this file instead of stdout'
    e longOpt: 'print-env', argName: 'printEnv', 'Print env before running command'
}

options = cli.parse(args)
if (!options || !options.c) {
    cli.usage()
    return
}

//exit with this code on timeout
int TIMEOUT_EXIT_CODE = 100
NAME = "timeout_wrapper"
timeoutOccurred = false

def duration(def seconds) {
    sprintf '%02d:%02d:%02d', (int) (seconds / 3600), (int) (seconds % 3600 / 60), (int) (seconds % 60)
}

def executeAndShowStreamingOutput(def command = []) {
    def timeout = Integer.parseInt(options.t)

    Process process
    ProcessBuilder builder = new ProcessBuilder(command)
    println "${NAME}: Executing: ${command}"
    println "${NAME}: Timeout in: ${duration(timeout)} ${options.i ? ' of inactivity': ''}"

    if (options.e) {
        println builder.environment()
    }

    def timeoutTask = {
        timeoutOccurred = true
        println "${NAME}: Timeout reached after ${duration(timeout)}${options.i ? ' of inactivity': ''}, performing handler tasks"

        SystemOutputInterceptor interceptor = null
        if (options.f) {
            File logFile = new File(options.f)
            println "${NAME}: Redirecting output of kill Command to file ${logFile}"
            interceptor = new SystemOutputInterceptor({id, text -> logFile << text ; false})
            interceptor.start()
        }
        if (!options.k) {
            println "${NAME}: Running JPS"
            runJPS()
        } else {
            println "${NAME}: Running: ${options.k}"
            options.k.execute().text
        }
        if (interceptor) {
            interceptor.stop()
        }
        println "${NAME}: Done with handler steps, aborting process"
        process.destroy()
    }

    Timer timer = new Timer()
    TimerTask watcherTask = timer.runAfter(timeout * 1000, timeoutTask)

    def timerReschedule = {
        if (!timeoutOccurred) {
            synchronized (timer) {
                watcherTask.cancel()
                watcherTask = timer.runAfter(timeout * 1000, timeoutTask)
            }
        }
    }

    if (options.i) {
        // Inactivity mode (timeout on no stdout/stderr)
        process = builder.start()
        process.consumeProcessOutput(new MyOutputStream(System.out, timerReschedule), new MyOutputStream(System.err, timerReschedule))
    } else {
        process = builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start()

    }

    process.waitFor()
    watcherTask.cancel()
    return process.exitValue()
}

/**
 * A little better than execute().text in case there is stderr coming out of jps
 */
def runCommandAndPrintOutput(def command) {
    Process process = command.execute()
    def out = new StringBuffer()
    def err = new StringBuffer()
    process.consumeProcessOutput( out, err )
    process.waitFor()
    if( out.size() > 0 ) println out
    if( err.size() > 0 ) println err
}

/**
 * Convenience method for debugging java hanging
 * @return
 */
def runJPS() {
    "jps -l".execute().text.eachLine { line ->
        (line =~ /^(\d+)\h+(.*)$/).with { m ->
            if (m.matches()) {
                if (!m[0][2].endsWith('.Jps') && !m[0][2].contains('swarm-client')) {
                    println ""
                    println "========================================="
                    println "=== [${m[0][1]}] : ${m[0][2]} ==================="
                    println "========================================="
                    runCommandAndPrintOutput("jstack ${m[0][1]}")
                }
            }
        }
    }
}

/* Begin main script execution */
long start = System.currentTimeSeconds()
def code = executeAndShowStreamingOutput(options.c.split())
println "${NAME}: Main execution finished with code: ${code}"
if (timeoutOccurred) {
    println "${NAME}: Execution was aborted due to timeout after ${System.currentTimeSeconds() - start} seconds, exiting with code ${TIMEOUT_EXIT_CODE}"
    System.exit(TIMEOUT_EXIT_CODE)
}

System.exit(code)
/* End main script execution */

/**
 * Wrap normal STDOUT/STDERR so we know that output is happening and reset
 * timeout when using "inactivity" mode
 */
class MyOutputStream extends PrintStream {
    private Closure outputSeenAction

    MyOutputStream(OutputStream originalStream, Closure outputSeenAction) {
        super(originalStream)
        this.outputSeenAction = outputSeenAction
    }

    @Override
    void write(int b) {
        super.write(b)
        outputSeenAction()
    }

    @Override
    void write(byte[] buf, int off, int len) {
        super.write(buf, off, len)
        outputSeenAction()
    }

    @Override
    void write(byte[] b) throws IOException {
        super.write(b)
        outputSeenAction()
    }
}
