package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.network.{Connector, PowerNode}
import li.cil.oc.common
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.property.PropertyRunning
import li.cil.oc.common.tileentity.capabilities.ColoredImpl
import li.cil.oc.util.DyeUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._

class Case(var tier: Int) extends traits.PowerAcceptor with traits.Computer with ColoredImpl with internal.Case with DeviceInfo {
  def this() = this(0)

  // Used on client side to check whether to render disk activity/network indicators.
  var lastFileSystemAccess = 0L
  var lastNetworkActivity = 0L

  setColor(DyeUtils.rgbValues(DyeUtils.byTier(tier)))

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Computer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Blocker",
    DeviceAttribute.Capacity -> getSizeInventory.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != getFacing

  override protected def connector(side: EnumFacing) = Option(if (side != getFacing && machine != null) machine.node.asInstanceOf[PowerNode] else null)

  override def energyThroughput = Settings.get.caseRate(tier)

  def isCreative = tier == Tier.Four

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.getNode != null && env.getNode.getAddress == address))

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer && isCreative && getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      // Creative case, make it generate power.
      getNode.asInstanceOf[PowerNode].changeBuffer(Double.PositiveInfinity)
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override protected def onRunningChanged(): Unit = {
    super.onRunningChanged()
    getBlockType match {
      case block: common.block.Case => getWorld.setBlockState(getPos, getWorld.getBlockState(getPos).withProperty(PropertyRunning.Running, Boolean.box(isRunning)))
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private final val TierTag = Settings.namespace + "tier"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    tier = nbt.getByte(TierTag) max 0 min 3
    setColor(DyeUtils.rgbValues(DyeUtils.byTier(tier)))
    super.readFromNBTForServer(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    nbt.setByte(TierTag, tier.toByte)
    super.writeToNBTForServer(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      if (InventorySlots.computer(tier)(slot).slot == Slot.Floppy) {
        common.Sound.playDiskInsert(this)
      }
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      val slotType = InventorySlots.computer(tier)(slot).slot
      if (slotType == Slot.Floppy) {
        common.Sound.playDiskEject(this)
      }
      if (slotType == Slot.CPU) {
        machine.stop()
      }
    }
  }

  override def getSizeInventory = if (tier < 0 || tier >= InventorySlots.computer.length) 0 else InventorySlots.computer(tier).length

  override def isUsableByPlayer(player: EntityPlayer) =
    super.isUsableByPlayer(player) && (!isCreative || player.capabilities.isCreativeMode)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, getClass)).fold(false)(driver => {
      val provided = InventorySlots.computer(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })
}
