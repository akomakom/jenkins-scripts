/**
 * Find jobs that do not discard old builds or keep too many.
 * Optionally configure them to discard builds (LogRotator/BuildDiscarder)
 */
import groovy.xml.*;
import hudson.model.*;
import hudson.maven.*;
import jenkins.model.*;
import jenkins.maven.*;
import hudson.*;
  
def count = 0
def buildWarning = 30 //anything over 30 is considered suspicious

Jenkins.instance.items.findAll{job ->  job instanceof Job && !job.isDisabled() }.each{
job ->
  count++
    //println("\nBegin Processing ${job.name} (${job.class}")
  def rotator = job.buildDiscarder
  
  if (rotator == null) {
    println "${job.fullName}: Not configured!"
    
    //set a new build discarder (uncomment to make changes)
    //job.setBuildDiscarder(new hudson.tasks.LogRotator(-1, 7, -1, -1))
    //println "Added one..."
  } else {
    if ( (rotator.numToKeep <= 0 && rotator.daysToKeep <= 0) || rotator.numToKeep > buildWarning || rotator.daysToKeep > buildWarning) {
      println "${job.fullName}: Problematic: Num: ${rotator.numToKeep} / Days: ${rotator.daysToKeep}"
    }
  }
}
count



/** 
* Change maven settings provider from FilePath to Default
*/

import groovy.xml.*;
  
import hudson.model.*;
import hudson.maven.*;
import jenkins.model.*;
import jenkins.maven.*;
import hudson.*;
  


def count = 0
Jenkins.instance.items.findAll{job ->  job instanceof FreeStyleProject && !job.isDisabled() }.each{
job ->
  count++
    //println("\nBegin Processing ${job.name} (${job.class}")
  
  job.builders.findAll{builder -> builder instanceof hudson.tasks.Maven && builder.settings instanceof jenkins.mvn.FilePathSettingsProvider}.each { builder ->
    println "Project ${job.name}"
    println("Settings: ${builder.settings}, path: ${builder.settings.path}")
    builder.settings = new jenkins.mvn.DefaultSettingsProvider()
  }
  
}

count





/**
 * Set Subversion credentialsId for jobs that use a particular URL and don't currently have one set
*/
import groovy.xml.*;
  
import hudson.model.*;
import hudson.maven.*;
import jenkins.model.*;
import jenkins.maven.*;
import hudson.*;
  
def selectFrom = Jenkins.instance.allItems
def credentialsId = 'some-cred-id-string'
def svnHostname = 'svn.hostname.for.your.company'

def count = 0
selectFrom.findAll{job -> job instanceof AbstractProject && !job.isDisabled() && job.scm instanceof hudson.scm.SubversionSCM }.each{ job ->
  
  job.scm.locations.findAll { loc ->  
    try { loc.SVNURL.host.startsWith(svnHostname) && (loc.credentialsId == null || loc.credentialsId == '') } catch (Exception x) { return false } 
  }.each{ loc ->
    
    println "JOB ${job.name}: [cred(${loc.credentialsId})] ${loc.SVNURL}"
    
    // Uncomment both lines when output looks right.
    //loc.credentialsId = null
    //loc.credentialsId = credentialsId
    count++
  }
}

count 
 
 
/** 
 * List all installed plugins so the text output can be diffed with another Jenkins 
 **/
Jenkins.instance.pluginManager.plugins.sort().each{
  plugin -> 
    println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
}
Jenkins.instance.pluginManager.plugins.size()
 
 

/**
 * Delete all DISABLED jobs in a view
 */
Hudson.instance.getView("VIEW NAME").getItems().findAll{job -> job instanceof Job && job.isDisabled()}.each{job ->
  println "Job ${job.fullName}"
  
  //job.delete() 
}


true



/**
 * Change all jobs tied to a deprecated labels to a new label (for simple labels)
 */
 Jenkins.instance.items.findAll{job ->  job instanceof Job  && job.assignedLabel?.expression == 'OLDLABEL'}.each{
job ->
  
  println "${job.assignedLabel}: ${job.assignedLabel?.class}"
  if (job.assignedLabel instanceof hudson.model.labels.LabelAtom ) {
    job.assignedLabel = new hudson.model.labels.LabelAtom('NEWLABEL')
  }
}

true



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


/** 
 * Disable all jobs in view and mention why
 */
Hudson.instance.getView("VIEW NAME").getItems().findAll{job -> job instanceof Job && !job.isDisabled()}.each{job ->
  println "Job ${job.fullName}"
  job.description = "${job.description} Disabling job, end of life"
  job.makeDisabled(true)
}
true
