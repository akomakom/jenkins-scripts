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
  

def urlPrefix = 'https://svn.url.for.my.company.that.requires.auth'
def credentialsId = 'some-string' //get from Credentials page in Jenkins (in url)

def count = 0
Jenkins.instance.items.findAll{job ->  job instanceof Project && !job.isDisabled() }.each{ job ->
  count++
    //println("\nBegin Processing ${job.name} (${job.class}")
  
  job.SCMs.findAll{scm  -> scm instanceof hudson.scm.SubversionSCM }.each { scm ->
    //println "Project ${job.name}"
    //println("SCM: ${scm.locations}")
    scm.locations.findAll{loc -> (loc.credentialsId == null || loc.credentialsId == '') && loc.remote.startsWith(urlPrefix)}.each { loc ->
      println "Project ${job.name}"
      println "Location : ${loc}"
      println "Current Credential: ${loc.credentialsId}"
      //set it (uncomment when ready)
      //loc.credentialsId = credentialsId
      
    }
  }
  
}

count




