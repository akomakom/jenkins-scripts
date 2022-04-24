
/**
 * List all installed plugins so the text output can be diffed with another Jenkins
 **/
Jenkins.instance.pluginManager.plugins.sort().each{
    plugin ->
        println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
}
Jenkins.instance.pluginManager.plugins.size()


