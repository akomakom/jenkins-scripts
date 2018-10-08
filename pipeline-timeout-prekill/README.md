Various attempts to solve the problem outlined in https://issues.jenkins-ci.org/browse/JENKINS-53315

Current status:
* pipeline-timeout-prekill-pure-dsl-failfast.groovy : works
* pipeline-timeout-prekill-pure-dsl.groovy : works but requires a clear kill strategy
* pipeline-timeout-prekill.sh : Does not seem to work in Jenkins 
* pipeline-timeout-prekill.groovy : Does not seem to work in Jenkins 

When I say "work in Jenkins", I mean that running this from within a "timeout" step causes the trap to either not run or run too late
