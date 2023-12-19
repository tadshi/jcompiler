package com.front;
import com.cotoj.JavaBytecodeSummoner;
import com.front.gunit.CompUnit;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class App {
    public static void main(String[] args) throws Exception {
        RandomAccessFile rfile;
        if (args.length == 1 && ("-v".equals(args[0]) || "--version".equals(args[0]))) {
            System.out.println("Jcompiler ver 0.2 by yifeiHuang623 and tadshi");
            return;
        } else if (args.length != 3) {
            System.out.println("Usage: java -jar jcompiler.jar <soruce_file> <output_file> <log_path>");
            return;
        }  else if (args[0].startsWith("--test=")) {
            String testFileName = "/test" + args[0].substring(7) + ".txt";
            System.out.println("Testing on " + testFileName.substring(1) + "...");
            File tempFile = File.createTempFile("jcompilertest", ".txt");
            FileUtils.copyInputStreamToFile(App.class.getResourceAsStream(testFileName), tempFile);
            rfile = new RandomAccessFile(tempFile, "r");
        }else {
            rfile = new RandomAccessFile(args[0], "r");
        }
        Path savePath = Path.of(args[1]);
        Path logPath = Path.of(args[2]);
        // 词法分析
        LexicalParser lexical = new LexicalParser(rfile);
        Map<Integer, LexicalParser.Word> wordMap = lexical.lexicalAnalysis();

        GrammaticalParser grammer = new GrammaticalParser(wordMap);
        CompUnit root = grammer.GrammaticalAnalysis();
        
        JavaBytecodeSummoner summoner = new JavaBytecodeSummoner(logPath);
        summoner.summon(root);
        // summoner.save(savePath);
        summoner.saveJar(savePath);
    }
}
