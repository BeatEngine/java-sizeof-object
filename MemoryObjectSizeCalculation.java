package de.ubs.xdm.utils.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MemoryObjectSizeCalculation {

    private HashSet<Object> visitedObjects;

    private Object root;

    private long size = 0;

    public MemoryObjectSizeCalculation(final Object root) {
        this.root = root;
    }

    private long sizeof(Object o) {
        if (o instanceof Integer) {
            return Integer.BYTES;
        } else if (o instanceof Long) {
            return Long.BYTES;
        } else if (o instanceof Float) {
            return Float.BYTES;
        } else if (o instanceof Double) {
            return Double.BYTES;
        } else if (o instanceof Character) {
            return Character.BYTES;
        } else if (o instanceof Byte) {
            return Byte.BYTES;
        } else if (o instanceof CharSequence) {
            return ((CharSequence) (o)).length() + 4;
        } /* Native Arrays */ else if (o instanceof int[]) {
            return (long) ((int[]) (o)).length * Integer.BYTES + 4;
        } else if (o instanceof long[]) {
            return (long) ((long[]) (o)).length * Long.BYTES + 4;
        } else if (o instanceof float[]) {
            return (long) ((float[]) (o)).length * Float.BYTES + 4;
        } else if (o instanceof double[]) {
            return (long) ((double[]) (o)).length * Double.BYTES + 4;
        } else if (o instanceof byte[]) {
            return ((byte[]) (o)).length + 4;
        } else if (o instanceof char[]) {
            return (long) ((char[]) (o)).length * Character.BYTES + 4;
        } else if (o instanceof Object[]) {
            for (final Object obj : (Object[]) o) {
                if (visitedObjects.add(obj)) {
                    if (addSizeNeedToParse(obj)) {
                        parse(obj);
                    }
                }
            }
            return 4;
        } /* Collections, List, etc... */ else if (o instanceof Iterable) {
            for (final Object obj : (Iterable<? extends Object>) o) {
                if (visitedObjects.add(obj)) {
                    if (addSizeNeedToParse(obj)) {
                        parse(obj);
                    }
                }
            }
            return 4;
        }
        return 0;
    }

    /**
     * @param o
     * @return true --> no native Object, it needs to get parsed; false --> added size
     */
    private boolean addSizeNeedToParse(Object o) {
        final long sz = sizeof(o);
        if (sz == 0) {
            // For the reference
            size += 4;
            return true;
        }
        size += sz;
        return false;
    }

    private List<Field> getAllFields(Object o) {
        List<Field> f = new ArrayList<>();
        Class<?> aClass = o.getClass();

        for (Field field : aClass.getDeclaredFields()) {
            f.add(field);
        }

        for (Field field : aClass.getFields()) {
            if (!f.contains(field)) {
                f.add(field);
            }
        }

        return f;
    }

    private void parse(final Object someObject) {
        boolean couldBeNative = true;
        for (Field field : getAllFields(someObject)) {
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(someObject);
                couldBeNative = false;
            } catch (IllegalAccessException | InaccessibleObjectException e) {
                continue;
            }
            if (value != null) {
                if (visitedObjects.add(value)) {
                    if (addSizeNeedToParse(value)) {
                        parse(value);
                    }
                }
            }
        }
        if (couldBeNative) {
            addSizeNeedToParse(someObject);
        }
    }

    private long calculate() {
        visitedObjects = new HashSet<>();
        size = 0;
        parse(root);
        return size;
    }

    public long calculateObjectSize() {
        return calculate();
    }
}
