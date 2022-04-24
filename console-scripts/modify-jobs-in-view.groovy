
/**
 * Delete all DISABLED jobs in a view
 */
Hudson.instance.getView("VIEW NAME").getItems().findAll{job -> job instanceof Job && job.isDisabled()}.each{job ->
    println "Job ${job.fullName}"

    //job.delete()
}


true


/**
 * Disable all jobs in view and mention why
 */
Hudson.instance.getView("VIEW NAME").getItems().findAll{job -> job instanceof Job && !job.isDisabled()}.each{job ->
    println "Job ${job.fullName}"
    job.description = "${job.description} Disabling job, end of life"
    job.makeDisabled(true)
}
true


