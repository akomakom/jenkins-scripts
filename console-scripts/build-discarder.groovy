/**
 * Find jobs that do not discard old builds or keep too many.
 * Optionally configure them to discard builds (LogRotator/BuildDiscarder)
 */
def count = 0
def buildWarning = 30 //anything over 30 is considered suspicious

Jenkins.instance.getAllItems(Job).each{
    job ->
        count++
        //println("\nBegin Processing ${job.name} (${job.class}")
        def rotator = job.buildDiscarder
        def problem = false

        if (rotator == null) {
            println "${job.fullName}: Not configured!"
            problem = true
            //set a new build discarder (uncomment to make changes)
            //job.setBuildDiscarder(new hudson.tasks.LogRotator(-1, 7, -1, -1))
            //println "Added one..."
        } else {
            if ( (rotator.numToKeep <= 0 && rotator.daysToKeep <= 0) || rotator.numToKeep > buildWarning || rotator.daysToKeep > buildWarning) {
                println "${job.fullName}: Problematic: Num: ${rotator.numToKeep} / Days: ${rotator.daysToKeep}"
                problem = true
            }
        }

        if (problem) {
            println "Total builds: ${job.getBuilds().size()}"
        }
}
count
