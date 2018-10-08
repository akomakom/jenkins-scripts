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
    k longOpt: 'on-kill-command', args: 1,  argName: 'onKillCommand', 'Command to execute when killed'
}

def options = cli.parse(args)
if (!options) {
    return
}

def executeAndShowStreamingOutput(def command = []) {
    ProcessBuilder builder = new ProcessBuilder(command)
    println "Executing: ${command}"

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
}


CLEANUP_REQUIRED = true
Runtime.runtime.addShutdownHook {
    println "Shutting down..."
    if( CLEANUP_REQUIRED ) {
        println "Cleaning up by running: ${options.k}"
        options.k.execute().text
        println "Done cleaning up"

    }
}
executeAndShowStreamingOutput(options.c.split(' '))
CLEANUP_REQUIRED = false
