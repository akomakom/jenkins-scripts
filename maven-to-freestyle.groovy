/**
  maven-to-freestyle.groovy
  See https://github.com/akomakom/jenkins-scripts for details

This script converts Maven jobs that rely on unsupported JDK version (1.6 and 1.7) to FreeStyle jobs.
It is meant to be run as a System Groovy Script

Configuration options (via jenkins job parameters)

DRY_RUN (boolean) - do not make changes

MODE (choice) - Possible values:
  RENAME: rename matching jobs to "DEPRECATED-X", create new jobs with the original name (some relationships may point to disabled jobs)
  DELETE: delete old job, then create new one with the old name.  Solves wrong project relationship issues. Back up everything!
  KEEP: leave the old job alone, create new jobs named "X.new" 

DISABLE (boolean) - disable old job after processing.  
  DISABLE=false and MODE=keep will allow this script be run over and over while testing

FOLDER (string) - restrict processing to the named folder (Cloudbees Folder Plugin).  Use "TOP" to disable folder processing (top-level only).
VIEW (string) - restrict processing to items in the named view.  Use DRY_RUN to check what will be processed


What this actually does:
1) moves maven job's prebuilders to steps
2) moves the main maven configuration to a maven build step (some config is lost as it's not supported)
3) moves maven job's postbuilders to steps
4) keeps everything else the same, including publishers, properties, etc.
5) attempts to take any maven-inferred downstream releationships (pom dependencies) and keep them 
 as static job relationships for the new jobs.  Note that in KEEP mode, these relationships will point to the original name.

NOTE: JDK names are hardcoded below!

*/


import groovy.xml.*;
  
import hudson.model.*;
import hudson.maven.*;
import jenkins.model.*;
import jenkins.maven.*;
import hudson.*;
  
def makeChanges = (build.buildVariableResolver.resolve("DRY_RUN") == "false")
def mode = build.buildVariableResolver.resolve("MODE")
def disable = (build.buildVariableResolver.resolve("DISABLE") == "true")
def restrictToFolder = build.buildVariableResolver.resolve("FOLDER")
def restrictToView = build.buildVariableResolver.resolve("VIEW")


if (!makeChanges) {
  println "\n\nDry run mode, add boolean build param 'DRY_RUN' to control\n\n"
}
println "MODE is ${mode}, disable old jobs is ${disable}"


def selectFrom = Jenkins.instance.allItems
def createIn

if (restrictToFolder != null && restrictToFolder != '') {
  
  if (restrictToFolder == 'TOP') {
    selectFrom = Jenkins.instance.items
    println "Restricting execution to the top level and ignoring folders"
  } else { 
    
    def folder = Jenkins.instance.getItemByFullName(restrictToFolder)
    if (folder == null) {
      println "Folder not found"
      return
    }    
    
    selectFrom = folder.items
    println "Restricting execution to folder ${restrictToFolder}, found ${selectFrom.size()} items"
  }
} else if (restrictToView != null && restrictToView != '') {
  
	def view = Hudson.instance.getView(restrictToView)
  	if(view == null) {
    	println "View ${restrictToView} not found"
    	return
  	}
 
	selectFrom = view.getItems()
  	println "Restricting execution to view ${restrictToView}, found ${selectFrom.size()} items"
}


def count = 0
selectFrom.findAll{job -> job instanceof MavenModuleSet && job.JDK && (job.JDK.name.indexOf('1.6') || job.JDK.name.indexOf('1.7')) > 0 && !job.isDisabled() }.each{
job ->
  count++
  println("\nBegin Processing ${job.fullName}")
  
  oldName = job.name
  createIn = job.parent
  
  xml = getModifiedXml(job)
  
  // write old and new configurations to files for historical reference
  getWorkspaceFile("${job.name}-config.old.xml").write(job.getConfigFile().asString(), null)
  getWorkspaceFile("${job.name}-config.new.xml").write(xml, null)
  
  println "Processed ${job.fullName}, new xml size is ${xml.length()}"
  
  
  if (makeChanges) {
    
    switch(mode) {
      
      case 'DELETE':
      	job.delete()
      	newName = oldName
      	break
          
      case 'RENAME':
      	newName = oldName
      	
        //rename old job
        job.renameTo("DEPRECATED-${job.name}")
      	break
      
        
      case 'KEEP':
        newName = "${oldName}.new"
      	existingNewJob = createIn.getItem(newName)
      	if (existingNewJob) {
          println "Job ${newName} already exists, deleting"
          existingNewJob.delete()
        }
        
      	break
    
      default:
        println "Unknown mode '${mode}', please add build param MODE.  Supported choices: RENAME,KEEP"
      	return
    }

    if (disable) {
		job.makeDisabled(true)
    }
    
    //now 
    //push to a new job (can't overwrite with different job type)
    inputStream = new StringBufferInputStream(xml)
    createIn.createProjectFromXML(newName, inputStream)
    println "(Renamed?) and converted ${oldName} to ${newName}"
  }  
  
}
println "\nProcessing completed, ${count} jobs visited\n\n"



def getModifiedXml(job) {
  def parser = new XmlParser()
  def response = parser.parse(job.getConfigFile().readRaw())
    
  
  builders = response.appendNode(new QName('builders'), [:])
  
  
  // Move prebuilders to builders
  if (response.prebuilders && response.prebuilders[0]) {
    response.prebuilders[0].children().each{ step ->
      builders.append(step)
    }
    response.remove(response.prebuilders[0])
  }
  
  //Create a maven block
  mavenNode = builders.appendNode(
    new QName("hudson.tasks.Maven"),
    [:]
  )
  
  //mavenNode.appendNode(new QName('targets'), [:], response.goals[0].value())
  move(response.goals[0], mavenNode, 'targets')
  move(response.mavenName[0], mavenNode)
  if (response.rootPOM) {
  	move(response.rootPOM[0], mavenNode, 'pom')
  }
  if (response.mavenOpts) {
    move(response.mavenOpts[0], mavenNode, 'jvmOptions')
  }
  move(response.settings[0], mavenNode)
  move(response.globalSettings[0], mavenNode)
  
  //items that don't exist in maven step:
  remove(response, [
    'resolveDependencies', 
    'processPlugins', 
    'siteArchivingDisabled', 
    'archivingDisabled', 
    'mavenValidationLevel', 
    'disableTriggerDownstreamProjects', 
    'blockTriggerWhenBuilding', 
    'fingerprintingDisabled', 
    'incrementalBuild', 
    'processPlugins', 
    'siteArchivingDisabled', 
    'ignoreUpstremChanges', 
    'rootModule'
    ])
  
  
  
  // Move postbuilders to builders
  if (response.postbuilders && response.postbuilders[0]) {
    response.postbuilders[0].children().each{ step ->
      builders.append(step)
    }
    response.remove(response.postbuilders[0])
  }
  
  // If there are any inferred relationships with other maven projects,
  // convert them to hard relationships (permanently)
  // This may result in duplicate BuildTrigger's if job had an explicit one already.
  makeBuildTriggerNode(job.getDownstreamProjects(), response.publishers[0])
  
  
  response.description[0].setValue(response.description[0].value + " Job was converted from a Maven project on ${new Date()}.  Any maven job relationships were converted to build triggers.")
  
  
  //rename top-level element
  //new parent node
  newDoc = new groovy.util.Node(null, 'project')
  response.children().each{ c ->
    newDoc.append(c)
  }
  
  //println XmlUtil.serialize(newDoc)
  //println response.text() 
  
  return XmlUtil.serialize(newDoc)
}  


def makeBuildTriggerNode(downstreamJobs, appendTo) {
  if (downstreamJobs.size() == 0) {
    return;
  }
  jobNames = []
  downstreamJobs.each{ job ->
    jobNames << job.name
  }

  root = new groovy.util.Node(appendTo, "hudson.tasks.BuildTrigger")
  new groovy.util.Node(root, 'childProjects', jobNames.join(', '))
  // not adding the rest of the values since defaults should work?
  
  return root;
}


def remove(from, listOfNames) {
  //println from.children().findAll { listOfNames.contains(it.name()) }
  from.children().findAll { listOfNames.contains(it.name()) }.each{node ->
    //println "Removing: ${node.name()}"
    from.remove(node)
  }
}


def move(from, to, toName = null) {
  // if source is null, we don't need to do anything
  if (from) {
    if (toName == null) {
      //just move it, no name change
      from.parent().remove(from)
      to.append(from)
    } else {
      to.appendNode(new QName(toName), from.attributes(), from.value())
      from.parent().remove(from)
    }
  }
}

def getWorkspaceFile(name) {
 
  if(build.workspace.isRemote())
  {
      channel = build.workspace.channel;
      fp = new FilePath(channel, build.workspace.toString() + "/" + name)
  } else {
      fp = new FilePath(new File(build.workspace.toString() + "/" + name))
  }
  
  return fp
}
