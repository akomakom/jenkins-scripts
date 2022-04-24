

/**
 * Change all jobs tied to a deprecated label to a new label (for simple labels)
 */
Jenkins.instance.getAllItems(Job).findAll{job -> job.assignedLabel?.expression == 'OLDLABEL'}.each{
    job ->

        println "${job.assignedLabel}: ${job.assignedLabel?.class}"
        if (job.assignedLabel instanceof hudson.model.labels.LabelAtom ) {
            job.assignedLabel = new hudson.model.labels.LabelAtom('NEWLABEL')
        }
}

true



