
/**
 * Rename all jobs in folder from old to new naming convention
 */
def folder = "publishers-4.1"
def oldVer = '4.1'
def newVer = '2.8.x'
def searchString = 'ehcache-'

def items = jenkins.model.Jenkins.getActiveInstance().getAllItems(hudson.model.AbstractItem.class)
items.each { item ->
//        println item.fullName
    if (item.fullName.startsWith("${folder}/")) {
        //any of the axis-tied jobs should be skipped. THey share build numbers
        // and will cause a failure if they sort last (parent will be deleted first)
        if (item.fullName.indexOf("=") < 0 && item.fullName.indexOf("\$") < 0) {
            //println "Job ${item.fullName}"
            //println "Short ${item.name}"
            if (item.name.indexOf(searchString) >= 0 && item.name.indexOf(oldVer) > 0) {
                def newName = item.name.replace(oldVer, newVer)
                println "Renaming ${item.fullName} To ${newName}"
                item.renameTo(newName)
            }
        }
    }
}

true
