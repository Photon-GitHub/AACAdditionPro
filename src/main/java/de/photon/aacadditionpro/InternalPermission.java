package de.photon.aacadditionpro;

import lombok.Getter;
import org.bukkit.permissions.Permissible;

public enum InternalPermission
{
    BYPASS("aacadditionpro.bypass"),
    DEBUG("aacadditionpro.debug"),
    INFO("aacadditionpro.info"),
    SETVL("aacadditionpro.setvl"),
    TABLISTREMOVE("aacadditionpro.tablistremove");

    @Getter private final String realPermission;

    InternalPermission(final String realPermission)
    {
        this.realPermission = realPermission;
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(final Permissible permissible, final String permission)
    {
        return permission == null || permissible.hasPermission(permission);
    }

    /**
     * Generates the bypass permission from the id of a module.
     */
    public static String bypassPermissionOf(String moduleId)
    {
        return (InternalPermission.BYPASS.getRealPermission() + '.') + moduleId;
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(final Permissible permissible, final InternalPermission permission)
    {
        return permission == null || permission.hasPermission(permissible);
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public boolean hasPermission(final Permissible permissible)
    {
        return hasPermission(permissible, this.realPermission);
    }
}
