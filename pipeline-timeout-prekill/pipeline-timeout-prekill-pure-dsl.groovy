/**
 * This is a pure Jenkins pipeline DSL alternative to pipeline-timeout-prekill.groovy
 * This will work as long as you have a clear way of killing your process 
 * (or are willing to rely on Jenkins to kill all child processes)
 *  
 * This does not use timeout at all, but rather creates a watcher thread.
 */

node() {
    
    // Create a hanging script to test with that outputs STDOUT and STDERR
    writeFile file: 'test.sh', text:  '''
#!/bin/bash

while [ 1 ] ; do
    echo "Normal Output $(date)"
    echo "STDERR Output $(date)" >&1
    sleep 1
done
    '''
    
    timestamps() {
        def timeout = 5
        def done = false
        parallel main: {
            sh 'bash test.sh'
        }, watcher: {
            def endTime = System.currentTimeSeconds() + timeout
            waitUntil {
               sleep(3) //less output
               System.currentTimeSeconds() >= endTime || done
            }
            if (!done) {
               echo "Performing cleanup now, then killing it"
               sh "sleep 3" // simulate cleanup time
               echo "Done with cleanup, killing it"
               sh "pkill -f 'bash test.sh'"
            }
        }
        done = true
    }
    
}
