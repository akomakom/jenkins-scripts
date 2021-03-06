/**
* Unlike pipeline-timeout-prekill-pure-dsl.groovy,
* this script (meant to reside in a shared library but can be moved to a Jenkinsfile as well) 
* does not need to know how to kill the main process
* in fact, any pipeline steps can be used in both main and preTimeout closures.
* This is more or less a drop-in replacement for the timeout step
* 
* Usage from a Jenkinsfile:
*
*  MyLibrary.timeoutWithDebug(this, 600, {
        //do stuff, eg sh './gradlew'
     }, 
     { 
        // preTimeout logic
     }
  }
  
  
*/

    /**
     * Runs the given code with given timeout.  If timeout is reached, runs preTimeout before killing
     * https://github.com/akomakom/jenkins-scripts/blob/master/pipeline-timeout-prekill/pipeline-timeout-prekill-pure-dsl.groovy
     * @param context script context if this is used from a Library
     * @param timeoutSeconds 
     * @param main Closure to run the (main build)
     * @param preTimeout Closure to run before killing main steps in case of timeout.  
     * @return
     */
    static def timeoutWithDebug(def context, int timeoutSeconds, Closure main, Closure preTimeout = null) {

        def done = false

        if (!preTimeout) {
            preTimeout = .... // some default, eg run jps
        }

        context.parallel main: {
            with main
            done = true //stop watcher on success.  On failure, it will be killed via failFast
        }, timeoutWatcher: {
            def endTime = System.currentTimeSeconds() + timeoutSeconds
            context.echo "Will terminate main steps after ${timeoutSeconds} seconds at (${endTime})"
            context.waitUntil {
                //   sleep(5) //less output
                System.currentTimeSeconds() >= endTime || done
            }
            if (!done) {
                context.echo "Performing preTimeout steps now, then killing main"
                with preTimeout
                context.echo "Done with preTimeout steps, killing main because this parallel branch is exiting"
                //stop main thread because this one failed:
                context.error "Raising an error to make sure that the process is cleaned up"
            }
        }, failFast: true

        done = true
    }
