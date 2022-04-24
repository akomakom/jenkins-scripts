
/**
 * Turn all slaves back on after being temporarily offline (such as from Unreliable Slave Plugin)
 */
Hudson.instance.slaves.findAll{it.computer.isOffline()}.each{def slave ->
    def computer = slave.computer
    computer.setTemporarilyOffline(false)
}

