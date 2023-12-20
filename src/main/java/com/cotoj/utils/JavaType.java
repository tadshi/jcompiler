package com.cotoj.utils;

import com.cotoj.utils.ReturnType.JavaClass;

public final class JavaType {
    public static ReturnType STRING = new JavaClass("java/lang/String");
    public static ReturnType INTEGER = new JavaClass("java/lang/Integer");
    public static ReturnType FLOAT = new JavaClass("java/lang/Float");
    public static ReturnType BOOLEAN = new JavaClass("java/lang/Boolean");
    public static ReturnType OBJECT = new JavaClass("java/lang/Object");
    public static ReturnType THREAD = new JavaClass("java/lang/Thread");
    public static ReturnType LIST_INT = new JavaClass("java/util/List");
    public static ReturnType ARRLIST = new JavaClass("java/util/ArrayList");
    public static ReturnType CONCLIST = new JavaClass("java/util/concurrent/CopyOnWriteArrayList");
    public static ReturnType DICT_INT = new JavaClass("java/util/Map");
    public static ReturnType HASHDICT = new JavaClass("java/util/HashMap");
    public static ReturnType CONCDICT = new JavaClass("java/util/concurrent/ConcurrentHashMap");
    public static ReturnType LOCK_INT = new JavaClass("java/util/concurrent/locks/Lock");
    public static ReturnType LOCK = new JavaClass("java/util/concurrent/locks/ReentrantLock");
    public static ReturnType SEM = new JavaClass("java/util/concurrent/Semaphore");
}
