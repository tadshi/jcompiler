package com.cotoj.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public abstract class ClassMaker {
    protected ClassWriter classWriter;
    protected TraceClassVisitor tcv;
    protected CheckClassAdapter cv;
    
    public ClassMaker(File logFile) throws FileNotFoundException {
        PrintWriter logWriter = new PrintWriter(logFile);
        classWriter = new ClassWriter(0);
        tcv = new TraceClassVisitor(classWriter, logWriter);
        cv = new CheckClassAdapter(tcv);
    }

    public void masterUp() {
        cv.visitEnd();
    }

    public void save(Path path) throws IOException {
        File outFile = path.toFile();
        OutputStream jout = new FileOutputStream(outFile);
        jout.write(classWriter.toByteArray());
        jout.flush();
        jout.close();
    }

    public byte[] getBytes() {
        return classWriter.toByteArray();
    }
}
