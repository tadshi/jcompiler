package com.front;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class LexicalParser {
    Map<Integer, Word> wordMap = new HashMap<Integer, Word>();
    int index = -1;
    int line = 0;
    RandomAccessFile file;

    public LexicalParser(RandomAccessFile file){
        this.file = file;
    }

    /**
     * Word：词法分析需要的信息
     */
    public class Word {
        // 位置，具体内容，类型和行号
        int id;
        String content;
        String type;
        int line;

        public Word(int id, String type, String content, int line){
            this.id = id;
            this.content = content;
            this.type = type;
            this.line = line;
        }
    }

    public Map<Integer, Word> lexicalAnalysis() throws IOException{
        String str = "";
        String s;
        int id = 0;
        int pos;
        char ch;
        while((pos = file.read()) != -1){
            ch = getChar();
            if (isBlank(ch)) continue;
            else if (isLetter(ch)) {
                str = dealWithLetter(ch);
            } else if (isDigit(ch)) {
                str = dealWithDigit(ch);
            } else if (!(s = isSingleCh(ch)).isEmpty()) {
                str = s;
            } else if (!(s = isDoubleCh(ch)).isEmpty()) {
                str = s;
            } else if (isQuotation(ch)) {
                str = dealWithFormat();
            } else if (!(s = isSlash(ch)).isEmpty()) {  //判断是除号还是注释
                if (!s.equals("Anno")) {
                    str = s;
                } else {
                    continue;
                }
            }
            int sep = str.indexOf(" ");
            Word temp = new Word(id, str.substring(0, sep), str.substring(sep + 1, str.length()), line);
            wordMap.put(id, temp);
            id++;
        }

        return wordMap;
    }

    char getChar(){
        index++;
        char ch = 'h';
        try {
            file.seek(index);
            ch = (char) file.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ch;
    }

    Boolean isBlank(char ch) { //空格、制表符、换行符
        if (ch == ' ' || ch == '\t' || ch == '\n') return true;
        if (ch == '\r') {
            line++;  //换行，行号加一
            return true;
        }
        return false;
    }

    Boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || ch >= 'A' && ch <= 'Z' || ch == '_';
    }
    
    Boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
    
    String isSingleCh(char ch) {
        if (ch == '+') return "PLUS +";
        if (ch == '-') return "MINU -";
        if (ch == '*') return "MULT *";
        if (ch == '%') return "MOD %";
        if (ch == ';') return "SEMICN ;";
        if (ch == ',') return "COMMA ,";
        if (ch == '(') return "LPARENT (";
        if (ch == ')') return "RPARENT )";
        if (ch == '[') return "LBRACK [";
        if (ch == ']') return "RBRACK ]";
        if (ch == '{') return "LBRACE {";
        if (ch == '}') return "RBRACE }";
        if (ch == ':') return "COLON :";
        if (ch == '.') return "DOT .";
        return "";
    }
    
    String isDoubleCh(char ch) {
        if (ch == '&') {
            int nextch = getChar();
            if (nextch == '&') return "AND &&";
            else index--;
        }
        if (ch == '|') {
            int nextch = getChar();
            if (nextch == '|') return "OR ||";
            else index--;
        }
        if (ch == '<') {
            int nextch = getChar();
            if (nextch == '=') return "LEQ <=";
            else {
                index--;
                return "LSS <";
            }
        }
        if (ch == '>') {
            int nextch = getChar();
            if (nextch == '=') return "GEQ >=";
            else {
                index--;
                return "GRE >";
            }
        }
        if (ch == '=') {
            int nextch = getChar();
            if (nextch == '=') return "EQL ==";
            else if(nextch == '>') return "RET =>";
            else {
                index--;
                return "ASSIGN =";
            }
        }
        if (ch == '!') {
            int nextch = getChar();
            if (nextch == '=') return "NEQ !=";
            else {
                index--;
                return "NOT !";
            }
        }
        return "";
    }
    
    String dealWithDigit(char ch) {
        StringBuilder str = new StringBuilder();
        boolean floatFlag = false;
        //num = ch - '0';
        char c = ch;
        while (isDigit(c) || c == '.') {
            if (c == '.') floatFlag = true;
            str.append(c);
            c = getChar();
        }
        index--; //回退
        if(floatFlag) return "FLOATCON " + str;
        return "INTCON " + str;
    }
    
    String dealWithLetter(char ch) {
        StringBuilder str = new StringBuilder();
        //str = str + ch;
        char c = ch;
        while (isDigit(c) || isLetter(c)) {
            str.append(c);
            c = getChar();
        }
        index--;
        String type;
        if (!(type = checkKey(str.toString())).isEmpty()) return type + " " + str;
        return "IDENFR " + str;
    }
    
    String checkKey(String str) {
        if (str.equals("main")) return "MAINTK";
        if (str.equals("const")) return "CONSTTK";
        if (str.equals("int")) return "INTTK";
        if (str.equals("break")) return "BREAKTK";
        if (str.equals("continue")) return "CONTINUETK";
        if (str.equals("if")) return "IFTK";
        if (str.equals("else")) return "ELSETK";
        if (str.equals("while")) return "WHILETK";
        if (str.equals("getint")) return "GETINTTK";
        if (str.equals("printf")) return "PRINTFTK";
        if (str.equals("return")) return "RETURNTK";
        if (str.equals("void")) return "VOIDTK";
        if (str.equals("thread")) return "THREADTK";
        if (str.equals("run")) return "RUNTK";
        if (str.equals("shared")) return "SHAREDTK";
        if (str.equals("float")) return "FLOATTK";
        if (str.equals("bool")) return "BOOLTK";
        if (str.equals("string")) return "STRINGTK";
        if (str.equals("list")) return "LISTTK";
        if (str.equals("dict")) return "DICTTK";
        if (str.equals("def")) return "DEFTK";
        if (str.equals("fn")) return "FNTK";
        if (str.equals("lock")) return "LOCKTK";
        if (str.equals("semaphore")) return "SEMAPHORETK";
        if (str.equals("await")) return "AWAITTK";
        if (str.equals("signal")) return "SIGNALTK";
        if (str.equals("true")) return "TRUECON";
        if (str.equals("false")) return "FALSECON";
        return "";
    }
    
    String dealWithFormat() {
        StringBuilder str = new StringBuilder();
        char c = getChar();
        while (!isQuotation(c) || c == '\\') {
            if (c == '\\'){
                c = getChar();
                if(c == 'n') str.append('\n');
                else if(c == 't') str.append('\t');
                else if(c == 'r') str.append('\r');
                else if(c == 'b') str.append('\b');
                else if(c == 'f') str.append('\f');
                else if(c == '"') str.append('\"');
                else if(c == 27) str.append('\'');
                c = getChar();
            }else {
                str.append(c);
                c = getChar();
            }
        }
        return "STRCON " + str;
    }
    
    void dealWithAnno(int type) { //0为//,1为/*
        char ch;
        if (type == 0) {
            while ((ch = getChar()) != '\n');
            line++; //注释注意加一行
            return;
        } else {
            while ((ch = getChar()) != '*') {
                if (ch == '\r') line++;
            }
            if ((ch = getChar()) == '/') return;
            else {
                index--;
                dealWithAnno(1);
            }
        }
    }

    Boolean isQuotation(char ch) { //双引号
        if (ch == 34) return true;
        return false;
    }

    String isSlash(char ch) {
        if (ch == '/') {
            char c = getChar();
            if (c == '/') {
                dealWithAnno(0);
                return "Anno";
            } else if (c == '*') {
                dealWithAnno(1);
                return "Anno";
            } else {
                index--;
                return "DIV /";
            }
        }
        return "";
    }
    
}
