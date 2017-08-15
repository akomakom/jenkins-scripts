/**
 * Find jobs that do not discard old builds or keep too many.
 * Optionally configure them to discard builds (LogRotator/BuildDiscarder)
 */
def count = 0
def buildWarning = 30 //anything over 30 is considered suspicious

Jenkins.instance.items.findAll{job ->  job instanceof Job }.each{
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




/**
 * Find original build in the pipeline that caused this build to be triggered
 * (build is the current build)
 */
  def prevRun = build
  def firstRun = prevRun

  while (prevRun != null)  {
    firstRun = prevRun
    def cause = prevRun.getCause(Cause.UpstreamCause)
    prevRun = cause?.upstreamRun
    println "Found run: ${firstRun}"
  }
  //Print out overall pipeline duration (may be useful in email templates)
  def duration = build.startTimeInMillis + build.duration - firstRun.startTimeInMillis
  println "Duration: ${hudson.Util.getTimeSpanString(duration)}"
  


/**
 * Find all gradle jobs (using gradle step or calling gradle on cmd-line)
 */
jenkins.model.Jenkins.getActiveInstance().getAllItems(Project.class).findAll{job -> job.builders.find{builder -> builder instanceof hudson.plugins.gradle.Gradle} }.each{job ->
  println "Found gradle job ${job.fullName}: ${job}"
}
jenkins.model.Jenkins.getActiveInstance().getAllItems(Project.class).findAll{job -> job.builders.find{builder -> builder instanceof hudson.tasks.CommandInterpreter && builder.command.indexOf('gradle') >= 0} }.each{job ->
  
  println "Found gradle job (cmd line) ${job.fullName}: ${job}"
}
true


/**
 * Turn all slaves back on after being temporarily offline (such as from Unreliable Slave Plugin)
 */
Hudson.instance.slaves.findAll{it.computer.isOffline()}.each{def slave ->
   def computer = slave.computer
   computer.setTemporarilyOffline(false)
}


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
 * Generate a cleanup script to remove orphaned "builds" subdirectories in disabled jobs.
 * Results can be pasted into a shell.  Threshold is in the if statement.
 * This script assumes that all jenkins builds have already been properly deleted for the affected jobs, and it 
 * suggests that you blindly remove any subdirectories named "builds" (which are sometimes orphaned)
 */
 Jenkins.instance.items.findAll{job -> job instanceof Job && job.isDisabled()}.each{job ->
//  println "Job ${job.fullName}"
   result =  "du -s /data/jenkins/jobs/${job.fullName}".execute().text
   parts = result.tokenize()
   if (Integer.parseInt(parts[0]) > 10000) {
     println "df /data ; find /data/jenkins/jobs/${job.fullName} -name builds -exec rm -rf {} \\;  ; df /data # ${parts[0]}"
   }
}.size()


/**
 * Print out Test Count summaries for a whole folder, based on looking at the latest completed build of each job
 * supports Pipeline jobs
 */
def summarize(def startsWith, def verbose = false) {
  def jobs = 0
  def jobsWithResults = 0
  def total = 0
  def failed = 0
  def skipped = 0
  
  jenkins.model.Jenkins.getActiveInstance().getAllItems(Job).findAll{job -> !job.isDisabled() && job.fullName.startsWith(startsWith)}.each{job ->
    if (verbose) { print "${job.fullName} => " }
    jobs++
    def build = job.getLastCompletedBuild()
    if (!build) {
        return
    }
    if (build.respondsTo('getTestResultAction') && build.getTestResultAction()) {
          
      total += build.getTestResultAction().getTotalCount()
      failed += build.getTestResultAction().getFailCount()
      skipped += build.getTestResultAction().getSkipCount()
      
      jobsWithResults++
      if (verbose) { 
        println "${build.getTestResultAction().getTotalCount()}: ${build.getTestResultAction().getFailCount()}, ${build.getTestResultAction().getSkipCount()}"
      }
      
    } else if (build.getActions()) {
      //println build.getActions()
      build.getActions(hudson.tasks.junit.TestResultAction).each {action ->
        
        total += action.getTotalCount()
        failed += action.getFailCount()
        skipped += action.getSkipCount()
        jobsWithResults++
        if (verbose) { 
          println "${action.getTotalCount()}: ${action.getFailCount()}/${action.getSkipCount()}"
        }
        
      }
    } else {
      if (verbose) { 
        println "NOTHING"
      }
    }
  }
  println "TOTAL FOR ${startsWith}:: ${jobs} Jobs (${jobsWithResults} with Test Results), Tests: ${total}: ${failed} Failed, ${skipped} Skipped"
}
  
summarize('FOLDER_NAME')
