package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.front.gunit.VarDef;

/**
 * StaticSummoner
 * Summon global variable / constant for the program.
 */
public class StaticSummoner extends ClassMaker implements Opcodes {
    MethodVisitor initVisitor;
    MethodHelper initHelper;
    public StaticSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_ABSTRACT, "com/oto/Static", null, "java/lang/Object", null);
        initVisitor = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        initVisitor.visitCode();
        initVisitor.visitVarInsn(ALOAD, 0);
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initHelper = new MethodHelper();
    }

    public void parseStaticDef(VarDef varDef, SymbolTable table) {
        IdentEntry entry = table.addVarDef(varDef);
        String descriptor = "I";
        if (entry.isArray()) {
            descriptor = "[".repeat(entry.getDimensions().size()) + descriptor;
            cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), descriptor, null, null).visitEnd();
            for (int dim : entry.getDimensions()) {
                initVisitor.visitIntInsn(SIPUSH, dim);
            }
            initHelper.reportUsedStack(entry.getDimensions().size());
            initVisitor.visitMultiANewArrayInsn(descriptor, entry.getDimensions().size());
            initVisitor.visitFieldInsn(Opcodes.PUTSTATIC, "com/oto/Static", entry.getName(), descriptor);
            // TODO
            
        } else {
            cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), descriptor, null, null).visitEnd();
        }
    }
}