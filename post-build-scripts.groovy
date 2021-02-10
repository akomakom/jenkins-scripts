/**
 * Scripts that make sense in the context of the jenkins global-post-script plugin
 */
 
 
/** 
 * Determine node(s) used by the build that just ran, whether it was a normal or pipeline job.
 * (and trigger some other job with the node as a paremeter) 
 *   This is useful for post-build cleanup
 */
 
import jenkins.model.*

import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepStartNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction

def jobToTrigger = 'slave-cleanup'
if (JOB_NAME == jobToTrigger) {
  println "Cleanup job will not trigger itself.  Done.";
  return;
}

def cleanupJob = Jenkins.instance.getItemByFullName(jobToTrigger)
println "Job name : ${JOB_NAME}";

def thisJob = Jenkins.instance.getItemByFullName(JOB_NAME);

if (manager.isNotBlankVar('NODE_NAME')) {
  // Normal Job
  cleanupJob.scheduleBuild2(0, new ParametersAction([ new StringParameterValue("NODE", NODE_NAME)]));
  println "Triggered cleanup job for ${NODE_NAME}"
} else if (thisJob instanceof WorkflowJob) {
  // PipelineJob
  def thisBuild = thisJob.getBuildByNumber(BUILD_NUMBER as Integer);
  println "Pipeline job: ${thisBuild}"
  exec = thisBuild.getExecution();
  if(exec == null)
    return;
  new FlowGraphWalker(exec)
    .findAll{it instanceof StepStartNode}
    .collect{it.getAction(WorkspaceAction)}
    .grep()
    .collectEntries{[(it.node.toString()): it.path.toString()]}
    .each{ node, workspace ->
        println "Triggering cleanup job for ${node}"
        cleanupJob.scheduleBuild2(0, new ParametersAction([ new StringParameterValue("NODE", node)]));
  }
}
