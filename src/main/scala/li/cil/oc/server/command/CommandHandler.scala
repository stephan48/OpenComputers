package li.cil.oc.server.command

import net.minecraftforge.fml.common.event.FMLServerStartingEvent

object CommandHandler {
  def register(e: FMLServerStartingEvent) {
    e.registerServerCommand(NonDisassemblyAgreementCommand)
    e.registerServerCommand(WirelessRenderingCommand)
    e.registerServerCommand(SpawnComputerCommand)
  }
}