package com.front;
import com.front.gunit.CompUnit;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class App {
    public static void main(String[] args) throws Exception {
        RandomAccessFile rfile;
        if (args.length != 1) {
            System.out.println("Usage: java -jar jcompiler.jar <target_file>");
            return;
        } else if ("-v".equals(args[0]) || "--version".equals(args[0])) {
            System.out.println("Jcompiler ver 0.2 by yifeiHuang623 and tadshi");
            return;
        } else if (args[0].startsWith("--test=")) {
            String testFileName = "/test" + args[0].substring(7) + ".txt";
            System.out.println("Testing on " + testFileName.substring(1) + "...");
            File tempFile = File.createTempFile("jcompilertest", ".txt");
            FileUtils.copyInputStreamToFile(App.class.getResourceAsStream(testFileName), tempFile);
            rfile = new RandomAccessFile(tempFile, "r");
        } else {
            rfile = new RandomAccessFile(args[0], "r");
        }
        // 词法分析
        LexicalParser lexical = new LexicalParser(rfile);
        Map<Integer, LexicalParser.Word> wordMap = lexical.lexicalAnalysis();

        GrammaticalParser grammer = new GrammaticalParser(wordMap);
        CompUnit root = grammer.GrammaticalAnalysis();

        System.out.println(root.getClass());
    }
}
