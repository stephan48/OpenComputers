package li.cil.oc.common.block

import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Disassembler extends SimpleBlock {
  override protected def customTextures = Array(
    None,
    Some("DisassemblerTop"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide")
  )

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.Disassembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":DisassemblerSideOn")
    Textures.Disassembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":DisassemblerTopOn")
  }

  // ----------------------------------------------------------------------- //

  override def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getUnlocalizedName, (Settings.get.disassemblerBreakChance * 100).toInt.toString))
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Disassembler()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Disassembler.id, world, x, y, z)
      }
      true
    }
    else false
  }
}
