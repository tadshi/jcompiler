package com.cotoj;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
public class Main implements Opcodes {
    private static String sekai = "World";
    public static void main(String[] args) {
        System.out.println("Hello " + sekai + "!!!");
        ClassWriter cw = new ClassWriter(0);
        PrintWriter pw = new PrintWriter(System.out);
        TraceClassVisitor tcv = new TraceClassVisitor(cw, pw);
        CheckClassAdapter cv = new CheckClassAdapter(tcv);
        cv.visit(V17, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "com/oto/Main", null, "java/lang/Object", null);
        cv.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "HIMITSU", "Ljava/lang/String;", null, "Hello World!").visitEnd();;
        var mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitFieldInsn(GETSTATIC, "com/oto/Main", "HIMITSU", "Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cv.visitEnd();
        
        File test = new File("Main.class");
        try {
            FileOutputStream oStream = new FileOutputStream(test);
            oStream.write(cw.toByteArray());
            oStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
