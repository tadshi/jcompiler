package com.cotoj.utils;

import com.cotoj.utils.ReturnType.JavaClass;

public final class JavaType {
    public static ReturnType STRING = new JavaClass("java/lang/String");
    public static ReturnType INTEGER = new JavaClass("java/lang/Integer");
    public static ReturnType OBJECT = new JavaClass("java/lang/Object");
    public static ReturnType THREAD = new JavaClass("java/lang/Thread");
    public static ReturnType LOCK_INT = new JavaClass("java/util/concurrent/locks/Lock");
    public static ReturnType LOCK = new JavaClass("java/util/concurrent/locks/ReentrantLock");
    public static ReturnType SEM = new JavaClass("java/util/concurrent/Semaphore");
}
