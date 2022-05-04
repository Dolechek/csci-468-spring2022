package edu.montana.csci.csci468.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CatscriptType {

    public static final CatscriptType INT = new CatscriptType("int", Integer.class);
    public static final CatscriptType STRING = new CatscriptType("string", String.class);
    public static final CatscriptType BOOLEAN = new CatscriptType("bool", Boolean.class);
    public static final CatscriptType OBJECT = new CatscriptType("object", Object.class);
    public static final CatscriptType NULL = new CatscriptType("null", Object.class);
    public static final CatscriptType VOID = new CatscriptType("void", Object.class);

    private final String name;
    private final Class javaClass;

    public CatscriptType(String name, Class javaClass) {
        this.name = name;
        this.javaClass = javaClass;
    }

    public boolean isAssignableFrom(CatscriptType type) {
        if (type == VOID) {
            return false;
        } else if (type == NULL) {
            return true;
        } else if (this.javaClass.isAssignableFrom(type.javaClass)) {
            return true;
        }
        return false;
    }

    // Memoization
    // not a thread safe implementation - alright for the purpose of Catscript
    private static Map<CatscriptType, ListType> cache = new HashMap();
    public static CatscriptType getListType(CatscriptType type) {
        // getting the current type.
        ListType listType = cache.get(type);
        // if there is a current type stored, then return that type.
        if (listType != null) {
            return  listType;
        }
        /* If there is no current type stored, then put the new type in the map
        * the key being type and the value stored being hasListType*/
        else {
            ListType hasListType = new ListType(type);
            cache.put(type, hasListType);
            return hasListType;
            //return new ListType(hasListType);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CatscriptType that = (CatscriptType) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Class getJavaType() {
        return javaClass;
    }

    public static class ListType extends CatscriptType {
        private final CatscriptType componentType;
        public ListType(CatscriptType componentType) {
            super("list<" + componentType.toString() + ">", List.class);
            this.componentType = componentType;
        }

        @Override
        public boolean isAssignableFrom(CatscriptType type) {
            if (type == NULL) {
                return true;
            } else if (type instanceof ListType) {
                ListType otherList = (ListType) type;
                return this.componentType.isAssignableFrom(otherList.componentType);
            }
            return false;
        }

        public CatscriptType getComponentType() {
            return componentType;
        }

        @Override
        public String toString() {
            return super.toString() + "<" + componentType.toString() + ">";
        }
    }

}
