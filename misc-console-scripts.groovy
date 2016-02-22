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
    
    // Uncomment when output looks right
    //loc.credentialsId = credentialsId
    count++
  }
}

count 
 
 
 



