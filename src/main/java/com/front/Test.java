package com.front;

import com.front.gunit.CompUnit;
import java.io.RandomAccessFile;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        RandomAccessFile file = new RandomAccessFile("src/main/resources/test1.txt", "r");

        // 词法分析
        LexicalParser lexical = new LexicalParser(file);
        Map<Integer, LexicalParser.Word> wordMap = lexical.lexicalAnalysis();

        GrammaticalParser grammer = new GrammaticalParser(wordMap);
        CompUnit root = grammer.GrammaticalAnalysis();

        System.out.println(root.getClass());
    }
}
