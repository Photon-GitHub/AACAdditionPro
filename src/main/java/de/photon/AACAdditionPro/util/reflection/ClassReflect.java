package de.photon.AACAdditionPro.util.reflection;

import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author geNAZt
 * @version 1.0
 */
public class ClassReflect
{
    private final Map<String, FieldReflect> cache = new ConcurrentHashMap<>();
    private final Map<Integer, FieldReflect> cacheIndex = new ConcurrentHashMap<>();

    private final Map<String, ConstructorReflect> constructorCache = new ConcurrentHashMap<>();

    private final Map<String, MethodReflect> methodCache = new ConcurrentHashMap<>();

    @Getter
    private final Class<?> clazz;

    ClassReflect(Class clazz)
    {
        this.clazz = clazz;
    }

    public FieldReflect field(String name)
    {
        FieldReflect fieldReflect = this.cache.get(name);
        if (fieldReflect == null) {
            try {
                Field field = this.clazz.getDeclaredField(name);
                field.setAccessible(true);
                fieldReflect = new FieldReflect(field);
                this.cache.put(name, fieldReflect);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return null;
            }
        }

        return fieldReflect;
    }

    public FieldReflect field(int index)
    {
        FieldReflect fieldReflect = this.cacheIndex.get(index);
        if (fieldReflect == null) {
            Field[] fieldArray = this.clazz.getDeclaredFields();
            if (fieldArray.length < index + 1) {
                return null;
            }

            Field field = fieldArray[index];
            field.setAccessible(true);
            fieldReflect = new FieldReflect(field);
            this.cacheIndex.put(index, fieldReflect);
        }

        return fieldReflect;
    }

    public ConstructorReflect constructor(Class... classes)
    {
        // Build the key first
        StringBuilder key = new StringBuilder();
        for (Class aClass : classes) {
            key.append(aClass.getName());
        }

        String cacheKey = key.toString();

        ConstructorReflect constructorReflect = this.constructorCache.get(cacheKey);
        if (constructorReflect == null) {
            // We need to search for the constructor now
            try {
                Constructor<?> constructor = this.clazz.getConstructor(classes);
                constructor.setAccessible(true);

                constructorReflect = new ConstructorReflect(constructor);
                this.constructorCache.put(cacheKey, constructorReflect);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return constructorReflect;
    }

    public MethodReflect method(String name)
    {
        MethodReflect methodReflect = this.methodCache.get(name);
        if (methodReflect == null) {
            for (Method method : this.clazz.getDeclaredMethods()) {
                // We take the first method with the name
                if (method.getName().equals(name)) {
                    method.setAccessible(true);

                    methodReflect = new MethodReflect(method);
                    this.methodCache.put(name, methodReflect);
                    return methodReflect;
                }
            }

            return null;
        }

        return methodReflect;
    }
}
