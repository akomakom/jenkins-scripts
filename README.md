# jenkins-scripts

Miscellaneous scripts for jenkins


maven-to-freestyle.groovy
-----------------------
* Jenkins groovy script to convert Maven projects that use JDK 1.6 to freestyle projects. 
* Jenkins > 1.609 no longer supports JDK 1.6 and neither do Maven projects. 
* Moves old jobs out of the way and creates new ones.
* Does not use the REST API, works directly with groovy by retrieving and modifying job XML
* To use, create a job with an "Execute System Groovy Script" step. Recommended config: DRY_RUN boolean parameter, archive *.xml for reference.


maven-to-freestyle-jobdsl.groovy
-----------------------
NOTE: this approach is deprecated, it's here for reference only.

* Jenkins Job DSL script to convert Maven projects that use JDK 1.6 to freestyle projects. 
* Jenkins > 1.609 no longer supports JDK 1.6 and neither do Maven projects. 
* Creates new projects named ?_F. 
* This is a work in progress that supports a small subset of all possible configuration options.
* NOTE: this is probably not what you want due to the aforementioned limitations, use the non-jobdsl script
