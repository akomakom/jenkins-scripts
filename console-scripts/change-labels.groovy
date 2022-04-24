

/**
 * Change all jobs tied to a deprecated label to a new label (for simple labels)
 */
Jenkins.instance.items.findAll{job ->  job instanceof Job  && job.assignedLabel?.expression == 'OLDLABEL'}.each{
    job ->

        println "${job.assignedLabel}: ${job.assignedLabel?.class}"
        if (job.assignedLabel instanceof hudson.model.labels.LabelAtom ) {
            job.assignedLabel = new hudson.model.labels.LabelAtom('NEWLABEL')
        }
}

true



