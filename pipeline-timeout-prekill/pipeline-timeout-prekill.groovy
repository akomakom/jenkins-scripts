/**
 * Simple wrapper script for running arbitrary shell command with pre-kill cleanup,
 * This is useful when running in a pipeline timeout{} step when you want to know why
 * your command is slow/hanging.  This script traps kill and runs a pre-kill command first.
 *
 * eg:
 * timeout(time:5, unit: 'MINUTES') {
 *   sh 'groovy this-script.groovy -c "./gradlew some tasks" -k "killall -3 java"
 * }
 *
 * If gradle takes more than 5 minutes, timeout will send a SIGTERM.   This script will intercept
 * the signal, run the -k command, then exit, killing the subprocess.
 */

// Parse command line
def cli = new CliBuilder(usage: '*.groovy -[ck] ')
cli.with {
    c longOpt: 'command', args: 1, argName: 'command', 'Command to execute'
    k longOpt: 'on-kill-command', args: 1,  argName: 'onKillCommand', 'Command to execute when killed.  Defaults to JPS analysis'
}

def options = cli.parse(args)
if (!options || !options.c) {
    cli.usage()
    return
}

NAME="kill_handling_wrapper"

def executeAndShowStreamingOutput(def command = []) {
    ProcessBuilder builder = new ProcessBuilder(command)
    println "${NAME}: Executing: ${command}"

    Process process = builder.start()

    InputStream stdout = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))

    InputStream stderr = process.getErrorStream()
    BufferedReader errReader = new BufferedReader(new InputStreamReader(stderr))

    def threadStderr = Thread.start {
        while((line = errReader.readLine()) != null ) {
            System.err.println(line)
        }
    }

    // Keep this on the main thread
    while((line = reader.readLine()) != null ) {
        System.out.println(line)
    }

    process.waitFor()
    return process.exitValue()
}

/**
 * Convenience method for debugging java hanging
 * @return
 */
def runJPS() {
    "jps -l".execute().text.eachLine { line ->
        (line =~ /^(\d+)\h+(.*)$/).with { m ->
            if ( m.matches() ) {
                if ( !m[0][2].endsWith('.Jps') ) {
                    println ""
                    println "========================================="
                    println "=== ${m[0][0]} ==================="
                    println "========================================="
                    println "jstack ${m[0][1]}".execute().text
                }
            }
        }
    }
}


CLEANUP_REQUIRED = true
Runtime.runtime.addShutdownHook {
    println "${NAME}: Shutting down... ${CLEANUP_REQUIRED}"
    if( CLEANUP_REQUIRED ) {
        if (!options.k) {
            println "${NAME}: Running JPS"
            runJPS()
        } else {
            println "${NAME}: Running: ${options.k}"
            options.k.execute().text
        }
        println "${NAME}: Done cleaning up"

    }
}
def code = executeAndShowStreamingOutput(options.c.split(' '))
println "${NAME}: Main execution finished: ${code}"
CLEANUP_REQUIRED = false

System.exit(code)
