package li.cil.oc.common.tileentity;

import li.cil.oc.OpenComputers$;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.api.tileentity.Rotatable;
import li.cil.oc.common.GuiType$;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.server.network.Waypoints;
import li.cil.oc.util.BlockPosUtils;
import li.cil.oc.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class TileEntityWaypoint extends AbstractTileEntitySingleNodeContainer implements BlockActivationListener, ITickable, RotatableImpl.RotatableHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Rotatable rotatable = new RotatableImpl(this);
    private final NodeContainer waypoint = new NodeContainerWaypoint(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_ROTATABLE = "rotatable";

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        if (isServer()) {
            Waypoints.add(this);
            getWorld().tickableTileEntities.remove(this); // TODO does this work and not blow up?
        }
    }

    @Override
    protected void dispose() {
        super.dispose();
        if (isServer()) {
            Waypoints.remove(this);
        }
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        nbt.setTag(TAG_ROTATABLE, rotatable.serializeNBT());
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        rotatable.deserializeNBT((NBTTagByte) nbt.getTag(TAG_ROTATABLE));
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return waypoint;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (!player.isSneaking()) {
            if (world.isRemote) {
                player.openGui(OpenComputers$.MODULE$, GuiType$.MODULE$.Waypoint().id(), world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        if (isServer()) {
            return;
        }

        final EnumFacing facing = rotatable.getFacing();
        final Vec3d origin = BlockPosUtils.getCenter(getPos()).addVector(
                facing.getFrontOffsetX() * 0.5,
                facing.getFrontOffsetY() * 0.5,
                facing.getFrontOffsetZ() * 0.5);

        final Random rng = getWorld().rand;
        final Vec3d delta = new Vec3d(
                rng.nextFloat() - 0.5f,
                rng.nextFloat() - 0.5f,
                rng.nextFloat() - 0.5f).
                scale(0.8f);
        final Vec3d pos = origin.add(delta);
        final Vec3d velocity = new Vec3d(
                rng.nextFloat() - 0.5f,
                rng.nextFloat() - 0.5f,
                rng.nextFloat() - 0.5f).
                scale(0.2f).
                addVector(
                        facing.getFrontOffsetX() * 0.3f,
                        facing.getFrontOffsetY() * 0.3f - 0.5f,
                        facing.getFrontOffsetZ() * 0.3f);

        getWorld().spawnParticle(EnumParticleTypes.PORTAL, pos.xCoord, pos.yCoord, pos.zCoord, velocity.xCoord, velocity.yCoord, velocity.zCoord);
    }

    // ----------------------------------------------------------------------- //
    // RotatableHost

    @Override
    public void onRotationChanged() {
//        final IBlockState state = getWorld().getBlockState(getPos());
//        getWorld().notifyBlockUpdate(getPos(), state, state, WorldUtils.FLAG_REGULAR_UPDATE);
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerWaypoint extends AbstractTileEntityNodeContainer {
        // ----------------------------------------------------------------------- //
        // Persisted data.

        private String label = "";

        // ----------------------------------------------------------------------- //
        // Computed data.

        // NBT tag names.
        private static final String TAG_LABEL = "label";

        private static final String NAME_WAYPOINT = "waypoint";

        // ----------------------------------------------------------------------- //

        NodeContainerWaypoint(final TileEntity host) {
            super(host);
        }

        // ----------------------------------------------------------------------- //

        @Callback(doc = "function(): string -- Get the current label of this waypoint.")
        public Object[] getLabel(final Context context, final Arguments args) {
            return new Object[]{label};
        }

        @Callback(doc = "function(value:string) -- Set the label for this waypoint.")
        public Object[] setLabel(final Context context, final Arguments args) {
            label = args.checkString(0);
            if (label.length() > 32) {
                label = label.substring(0, 32);
            }
            context.pause(0.5);
            return null;
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NETWORK).withComponent(NAME_WAYPOINT).create();
        }

        // ----------------------------------------------------------------------- //
        // INBTSerializable

        @Override
        public NBTTagCompound serializeNBT() {
            final NBTTagCompound nbt = super.serializeNBT();
            nbt.setString(TAG_LABEL, label);
            return nbt;
        }

        @Override
        public void deserializeNBT(final NBTTagCompound nbt) {
            super.deserializeNBT(nbt);
            label = nbt.getString(TAG_LABEL);
        }
    }
}
