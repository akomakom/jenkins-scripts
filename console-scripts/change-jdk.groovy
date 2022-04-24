def oldJDK = Jenkins.instance.getJDK('JDK_1.8')
def replacementJDK = Jenkins.instance.getJDK('ZULU_JDK_1.8')

Jenkins.instance.getAllItems(FreeStyleProject.class).findAll{job -> !job.isDisabled() && job.JDK == oldJDK }.each{ job ->
    println "${job.fullName} : ${job.JDK.name}"
    job.JDK = replacementJDK
    job.save()
}.size()