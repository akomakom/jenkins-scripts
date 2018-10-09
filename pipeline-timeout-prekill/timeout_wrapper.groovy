/**
 * Simple wrapper script for running arbitrary shell commands, killing them on timeout and 
 * performing custom logic just before timeout.
 * 
 * eg:
 *  sh 'groovy this-script.groovy -t 300 -c "./gradlew some tasks" -k "killall -3 java"
 *}*
 * If gradle takes more than 5 minutes, this script will perform -k and abort the process
 * Omitting -k causes the script to run the default "stacktrace on all java processes" handler
 */

// Parse command line
def cli = new CliBuilder(usage: '*.groovy -[ck] ')
cli.with {
    t longOpt: 'timeout', args: 1, 'Timeout in seconds'
    c longOpt: 'command', args: 1, argName: 'command', 'Command to execute'
    k longOpt: 'on-kill-command', args: 1, argName: 'onKillCommand', 'Command to execute when killed.  Defaults to JPS analysis'
}

options = cli.parse(args)
if (!options || !options.c) {
    cli.usage()
    return
}

NAME = "timeout_wrapper"
timeoutOccurred = false

def executeAndShowStreamingOutput(def command = []) {
    ProcessBuilder builder = new ProcessBuilder(command)
    println "${NAME}: Executing: ${command}"

    Process process = builder.start()


    InputStream stdout = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))

    InputStream stderr = process.getErrorStream()
    BufferedReader errReader = new BufferedReader(new InputStreamReader(stderr))

    TimerTask watcherTask = new Timer().runAfter(Integer.parseInt(options.t) * 1000, {
        timeoutOccurred = true
        println "${NAME}: Timeout reached after ${options.t}s, performing handler tasks"
        if (!options.k) {
            println "${NAME}: Running JPS"
            runJPS()
        } else {
            println "${NAME}: Running: ${options.k}"
            options.k.execute().text
        }
        println "${NAME}: Done with handler steps, aborting process"
        process.destroy()
    })

    def threadStderr = Thread.start {
        while ((line = errReader.readLine()) != null) {
            System.err.println(line)
        }
    }

    // Keep this on the main thread
    while ((line = reader.readLine()) != null) {
        System.out.println(line)
    }

    process.waitFor()
    watcherTask.cancel()

    return process.exitValue()
}

/**
 * Convenience method for debugging java hanging
 * @return
 */
def runJPS() {
    "jps -l".execute().text.eachLine { line ->
        (line =~ /^(\d+)\h+(.*)$/).with { m ->
            if (m.matches()) {
                if (!m[0][2].endsWith('.Jps')) {
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



def code = executeAndShowStreamingOutput(options.c.split(' '))
println "${NAME}: Main execution finished with code: ${code}"
if (timeoutOccurred) {
    println "${NAME}: Execution was aborted due to timeout"
}

System.exit(code)
