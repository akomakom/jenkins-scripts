import hudson.model.*;
import hudson.util.*;
import hudson.maven.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

count = 0
total = 0

//Class.forName('hudson.maven.MavenModuleSet')

for (item in Jenkins.instance.items) {
  jobName = item.getFullDisplayName()
  
  // If the installation uses cloudbees Folders, not everything is a Job
  if (!(item instanceof Job)) {
    println "Skipping ${item.name}, not a Job"
    continue
  }
  
  total++
  //println item.class.name
  
  /**/
  if (item.class.name.endsWith('hudson.maven.MavenModuleSet')) {
    jdk = item.getJDK()
    if (jdk != null && jdk.name.indexOf('1.6') > 0) {
      println "${count}: ${item.name} JDK ${jdk}"
      count++
      convert(item, "${item.name}_F")
        //println item.scm.locations[0].credentialsId
    }
  }
  /**/
}
println "Total: ${count} of ${total}"
  


void convert(item, newName) {
  println "\n\n\n---------------------\n\n\nConverting job ${item.name}"
  
  
  freeStyleJob(newName) {
    logRotator(-1, 10)
    jdk(item.JDK.name)
    label(item.getAssignedLabelString())
    
    oldscm = item.scm
    
    scm {
      if (oldscm.type == 'hudson.scm.SubversionSCM') {
        //create svn
        svn{ 
	        item.scm.locations.each{ loc ->
              location (loc.remote) {
                credentials(loc.credentialsId)
                directory(loc.local)
              }
              
            }
        }
        
        
        steps {
          item.prebuilders.each{ step ->
            println "Working on step ${step}"
            
            switch ("${step.class.name}") {
				case 'hudson.tasks.Shell':
              		shell(step.contents)
              		break
            	
                default:
		            println "Attention: Unsupported step: ${step.class.name}"
	    	      //TODO: support other steps
            }
          }
        
          
          //add maven
          maven {
            mavenInstallation(item.maven.name)
            goals(item.goals)
            mavenOpts(item.mavenOpts)
            
            //This business isn't even supported correctly in the newer maven plugin
            println "Local Repo is ${item.localRepository}"
            if (item.localRepository.toString().toLowerCase().indexOf('perexecutor') > 0) {
              localRepository(LocalRepositoryLocation.LOCAL_TO_EXECUTOR)
              println "Local Repo Set to executor"
            } else if (item.localRepository.toString().toLowerCase().indexOf('workspace') > 0) {
              localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
              println "Local Repo Set to workspace"
            }
            
            properties(item.mavenProperties)
            
            rootPOM(item.rootPOM)
          }
          
          
          item.postbuilders.each{ step ->
            println "Working on step ${step}"
            
            switch ("${step.class.name}") {
				case 'hudson.tasks.Shell':
              		shell(step.contents)
              		break
            	
                default:
		            println "Attention: Unsupported step: ${step.class.name}"
	    	      //TODO: support other steps
            }

          }
          
          println "Publishers: ${item.publishers}"
          
          publishers {
            item.publishers.each { pub ->
              println "Publisher ${pub.class}"
              
              switch("${pub.class.name}") {
                case 'hudson.plugins.emailext.ExtendedEmailPublisher': 
                  
                  extendedEmail(pub.recipientList, pub.defaultSubject, pub.defaultContent) {
                    
                    pub.configuredTriggers.each { tr ->
//                      println "\tAdding trigger ${tr.descriptor.displayName} -> ${mapEmailExtTriggerName(tr.descriptor.displayName)}"
                      email = tr.email
                      //println "Send to devs: ${email.sendToDevelopers}  vs ${email.getSendToDevelopers()}"
                      //println "Send to culprits: ${email.includeCulprits}  vs ${email.getSendToCulprits()}"
                      
                      // must use email.getSendToCulprits() vs email.includeCulprits since that is one field that doesn't match getter
                      trigger(triggerName: mapEmailExtTriggerName(tr.descriptor.displayName), subject: email.subject, body: email.body, recipientList:  email.recipientList,
                        sendToDevelopers: email.sendToDevelopers, sendToRequester: email.sendToRequester, includeCulprits: email.getSendToCulprits(), sendToRecipientList: email.sendToRecipientList)
                    }
                  }
                  break
                    
                case 'hudson.tasks.ArtifactArchiver': 
                      
                	archiveArtifacts {
                      // If set, does not fail the build if archiving returns nothing.
                      allowEmpty(pub.allowEmptyArchive)
                      // Uses default excludes.
                      defaultExcludes(pub.isDefaultExcludes())
                      // Specifies files that will not be archived.
                      exclude(pub.excludes)
                      // Fingerprints all archived artifacts.
                      fingerprint(pub.isFingerprint())
                      // Archives artifacts only if the build is successful.
                      onlyIfSuccessful(pub.isOnlyIfSuccessful())
                      // Specifies the files to archive.
                      pattern(pub.artifacts)
                    }
                
                	break
                
                case 'hudson.tasks.BuildTrigger':
                
                	downstream(pub.childProjectsValue, "${pub.threshold}") 
                
                	break
                	
                      
                case 'hudson.plugins.parameterizedtrigger.BuildTrigger':
                
                	downstreamParameterized {
                        pub.configs.each{	config ->
                            // Adds a trigger for parametrized builds.
                            trigger(config.projects, config.condition.displayName, config.triggerWithNoParameters) {
                                
                              config.configs.each { param ->
                                  //condition(config.condition.displayName)
                                  //triggerWithNoParameters(config.triggerWithNoParameters)
                                
                                    switch(param.class.simpleName) {
                                      /**/
                                      case 'BooleanParameters':
                                          param.configs.each { paramConfig ->
                                              boolParam(paramConfig.name, paramConfig.value)
                                          }
                                      break
                                        
                                      case 'CurrentBuildParameters':
                                          currentBuild()
                                          break
                                            
                                      case 'FileBuildParameters':
                                          propertiesFile(param.propertiesFile, param.failTriggerOnMissing)
                                          break
                                            
                                      case 'NodeParameters':
                                          sameNode(true)
                                          break
                                            
                                      case 'PredefinedBuildParameters':
                                          predefinedProps(param.properties)
                                          break
                                      
                                      case 'SubversionRevisionBuildParameters':
                                          subversionRevision(param.includeUpstreamParameters)
                                          break
                                            
                                      case 'GitRevisionBuildParameters':
                                      	  gitRevision(param.combineQueuedCommits)
                                      	  break 
                                            
                                      default:
                                          println "WARNING: unknown parameter type for downstream project: ${param.class}"
                                            
                                    }
                                  
                              }
                              
                              
                                // Passes the Git commit that was used in this build to the downstream builds. Deprecated.
                              //  gitRevision(boolean combineQueuedCommits = false)
                                // Specifies a Groovy filter expression that restricts the subset of combinations that the downstream project will run. Deprecated.
                              //  matrixSubset(String groovyFilter)
                            }
                        }
                    }
                
                	break
                default:
	                println "WARNING: unsupported publisher step: ${pub.class}"
                
                
                	
              }
              
            }
          }
        }
        
        
            
      }
    
    }
    
  }
  
}



/**
 * Maps jenkins name to DSL name
 */
public String mapEmailExtTriggerName(name) {
  map = [
    "Aborted" : "Aborted",
    "Always" : "Always",
    "Before Build" : "PreBuild",
    "Failure - 1st": "FirstFailure",
    "Failure - 2nd": "SecondFailure",
    "Failure - Any": "Failure",
    "Failure - Still": "StillFailing",
    "Failure -> Unstable (Test Failures)": "Unstable",
    "Fixed": "Fixed",
    "Not Built": "NotBuilt",
    "Status Changed": "StatusChanged",
    "Success": "Success",
    "Test Improvement": "Improvement",
    "Test Regression": "Regression",
    "Unstable (Test Failures)": "Unstable",
    "Unstable (Test Failures) - 1st": "FirstUnstable",
    "Unstable (Test Failures)/Failure -> Success": "FixedUnhealthy",
    "Unstable (Test Failures) - Still": "StillFailing",
  ]
  if (map[name]) {
    return map[name]
  }
  
  return "Failure"
  
  
}
