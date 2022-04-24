
/**
 * Disable and move matching jobs to a subfolder
 */
def jenkins = Jenkins.instance

def FOLDER_NAME = 'Archive'
def JOB_NAME_START = 'some_prefix'
def folder = jenkins.getItemByFullName(FOLDER_NAME)

println "Moving to folder ${folder}"

Jenkins.instance.items.findAll{job ->  job instanceof Job && job.name.startsWith(JOB_NAME_START) }.each{job ->
    println job.name
    job.description += "Disabled on ${new Date()} ${job.description}"
    job.makeDisabled(true)
    Items.move(job, folder)
}


/**
 * Move disabled top-level non-pipeline jobs to a subfolder
 */
def jenkins = Jenkins.instance

def FOLDER_NAME = 'Archive'
def folder = jenkins.getItemByFullName(FOLDER_NAME)

jenkins.getItems(Job.class).findAll{job -> job.isDisabled()}.each{job ->
    println job.fullName
    Items.move(job, folder)
}.size()

