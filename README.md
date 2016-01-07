# jenkins-scripts

Miscellaneous scripts for jenkins


maven-to-freestyle.groovy
-----------------------

Jenkins Job DSL script to convert Maven projects that use JDK 1.6 to freestyle projects. 
Jenkins > 1.609 no longer supports JDK 1.6 and neither do Maven projects. 
Creates new projects named ?_F. 
This is a work in progress that supports a small subset of all possible configuration options
