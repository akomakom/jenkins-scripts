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



