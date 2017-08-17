package de.photon.AACAdditionPro.util.reflection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Reflect
{

    private static final Map<String, ClassReflect> REFLECTION_CACHE = new ConcurrentHashMap<>();

    public static ClassReflect from(Class clazz)
    {
        // computeIfAbsent automatically puts the new value in the REFLECTION_CACHE.
        return REFLECTION_CACHE.computeIfAbsent(clazz.getName(), key -> new ClassReflect(clazz));
    }

    public static ClassReflect from(String classPath)
    {
        try {
            return from(Reflect.class.getClassLoader().loadClass(classPath));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ClassReflect fromNMS(String classPath)
    {
        return from(("net.minecraft.server." + ReflectionUtils.getVersionString()) + "." + classPath);
    }

    public static ClassReflect fromOBC(String classPath)
    {
        return from(("org.bukkit.craftbukkit." + ReflectionUtils.getVersionString()) + "." + classPath);
    }
}
