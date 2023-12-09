package com.cotoj.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public abstract class ClassMaker {
    protected ClassWriter classWriter;
    protected TraceClassVisitor tcv;
    protected CheckClassAdapter cv;
    
    public ClassMaker(File logFile) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(logFile);
        classWriter = new ClassWriter(0);
        tcv = new TraceClassVisitor(writer);
        cv = new CheckClassAdapter(tcv);
    }

    public void masterUp() {
        cv.visitEnd();
    }
}
