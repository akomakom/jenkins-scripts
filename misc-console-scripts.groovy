

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
Jenkins.instance.getAllItems(FreeStyleProject.class).findAll{job -> !job.isDisabled() }.each{
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
Jenkins.instance.getAllItems(AbstractProject).findAll{job -> !job.isDisabled() && job.scm instanceof hudson.scm.SubversionSCM }.each{ job ->
  
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
 * Generate a cleanup script to remove orphaned "builds" subdirectories in disabled jobs.
 * Results can be pasted into a shell.  Threshold is in the if statement.
 * This script assumes that all jenkins builds have already been properly deleted for the affected jobs, and it 
 * suggests that you blindly remove any subdirectories named "builds" (which are sometimes orphaned)
 */
Jenkins.instance.getAllItems(Job).findAll{job -> job.isDisabled()}.each{job ->
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
