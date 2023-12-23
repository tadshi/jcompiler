package com.cotoj.utils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
import com.front.gunit.FuncDef;

public interface ThreadHelper {
    public record WriterPack(ClassWriter cw, ClassVisitor cv) {}
    public static String getClassName(FuncDefNode funcDef) {
        if (!funcDef.isParallel()) {
            throw new RuntimeException("This is not a thread func!");
        }
        return "com/threads/Thread" + funcDef.getName();
    }
    
    public static String getClassName(FuncDef funcDef) {
        if (!(funcDef.isThread() || funcDef.isRoutine())) {
            throw new RuntimeException("This is not a thread func!");
        }
        return "com/threads/Thread" + funcDef.getIdent().getName();
    }

    public static String getInitDescriptor(FuncDefNode funcDef) {
        StringBuilder sb = new StringBuilder("(");
        for (FuncParamNode param : funcDef.getParams()) {
            sb.append(param.getDescriptor());
        }
        sb.append(")V");
        return sb.toString();
    }

    public static WriterPack summonThreadClass(FuncDefNode funcDef) {
        ClassWriter cw = new ClassWriter(0);
        CheckClassAdapter cv = new CheckClassAdapter(cw);
        cv.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, getClassName(funcDef), null, "java/lang/Object", new String[]{"java/lang/Runnable"});
        return new WriterPack(cw, cv);
    }
}
