package de.g4memas0n.services.util;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.jetbrains.annotations.NotNull;

/**
 * Permission Enum for all permission notes of this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public enum Permission {

    /**
     * Permission for filter service environments.
     *
     * <p><b>Note:</b> This permission is only functional when the permission per environment option is enabled.</p>
     *
     * Available environment permissions:
     * - {@code services.environment.nether} (Allows to use services in the {@link Environment#NORMAL} environment)
     * - {@code services.environment.normal} (Allows to use services in the {@link Environment#NETHER} environment)
     * - {@code services.environment.the_end} (Allows to use services in the {@link Environment#THE_END} environment)
     * - {@code services.environment.*} (Allows to use services in all service environments.)
     */
    ENVIRONMENT("environment"),

    /**
     * Permission for filter service items.
     *
     * <p><b>Note:</b> This permission is only functional when the permission per item option is enabled.</p>
     *
     * Available item permissions:
     * - {@code services.item.bedrock} (Allows to use {@link Material#BEDROCK} as service item)
     * - {@code services.item.wooden_axe} (Allows to use {@link Material#WOODEN_AXE} as service item)
     * - {@code services.item.<item-key>} (Allows to use the specified service item)
     * - {@code services.item.*} (Allows to use all service items)
     */
    ITEM("item"),

    /**
     * Permission for the reload command.
     */
    RELOAD("reload"),

    /**
     * Permission for using services.
     */
    SERVICE("service"),

    /**
     * Permission for the main services command.
     */
    USE("use"),

    /**
     * Permission for the version command.
     */
    VERSION("version"),

    /**
     * Permission for filter service worlds.
     *
     * <p><b>Note:</b> This permission is only functional when the permission per world option is enabled.</p>
     *
     * Available world permissions:
     * - {@code services.world.world} (Allows to use services in the world named {@code world})
     * - {@code services.world.world_nether} (Allows to use services in the world named {@code world_nether})
     * - {@code services.world.world_the_end} (Allows to use services in the world names {@code world_the_end})
     * - {@code services.world.<world>} (Allows to use services in the specified world)
     * - {@code services.item.*} (Allows to use services in all service worlds)
     */
    WORLD("world");

    private static final String DELIMITER = ".";
    private static final String PREFIX = "services";

    private final String node;

    Permission(@NotNull final String node) {
        this.node = PREFIX + DELIMITER + node;
    }

    /**
     * Returns the complete node of this permission.
     *
     * @return the permission node.
     */
    public @NotNull String getNode() {
        return this.node;
    }

    /**
     * Returns the children permission with the given sub-node.
     *
     * <p>Joins the node of this permission with the given sub-node.</p>
     *
     * @param children the sub-node to join.
     * @return the children permission node.
     */
    public @NotNull String getChildren(@NotNull final String children) {
        return this.node + DELIMITER + children.toLowerCase();
    }

    /**
     * Sets the children permission with the given sub-node.
     *
     * <p>Joins the node of this permission with the given sub-node and registers it to bukkit.</p>
     *
     * @param children the sub-node of the children permission.
     */
    public void setChildren(@NotNull final String children) {
        new org.bukkit.permissions.Permission(this.getChildren(children)).addParent(this.getWildcard(), true);
    }

    /**
     * Returns the wildcard permission of this node.
     *
     * <p>Joins the node of this permission with the wildcard symbol {@code *}.</p>
     *
     * @return the wildcards permission node.
     */
    public @NotNull String getWildcard() {
        return this.node + DELIMITER + "*";
    }
}
