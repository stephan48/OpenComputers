package li.cil.oc.api.driver;

import net.minecraft.item.ItemStack;

/**
 * NodeContainer providers allow OpenComputers to resolve item stacks to the
 * environments generated by the block or item component of the stack.
 * <p/>
 * This is mainly used in OpenComputers' JEI usage handler for displaying
 * a component's API.
 */
public interface EnvironmentProvider {
    /**
     * Get the environment provided by the specified stack.
     * <p/>
     * For block items this will usually be the tile entity.
     * <p/>
     * For items this will be the type of the environment returned by the
     * item driver's {@link DriverItem#createEnvironment} method.
     *
     * @param stack the stack to get the environment type for.
     * @return the environment type for the specified stack.
     */
    Class<?> getEnvironment(final ItemStack stack);
}
