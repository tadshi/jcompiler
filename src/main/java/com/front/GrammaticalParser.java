package com.front;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.*;
import com.front.gunit.FuncFParam.FuncParamType;

public class GrammaticalParser {
    CompUnit root;
    int treeId = 0;
    int grammarId = -1;
    Map<Integer, LexicalParser.Word> wordMap = new HashMap<Integer, LexicalParser.Word>();
    Map<Integer, ObjectClass> id2Object = new HashMap<Integer, ObjectClass>();
    public final List<CError> errors = new ArrayList<>();

    public GrammaticalParser(Map<Integer, LexicalParser.Word> wordMap){
        this.wordMap = wordMap;
    }


    public CompUnit GrammaticalAnalysis(){
        root = parsCompUnit();
        id2Object.put(treeId++, root);
        return root;
    }

    //CompUnit → {Decl} {FuncDef} MainFuncDef
    CompUnit parsCompUnit(){
        CompUnit ret = new CompUnit();
        // 是否继续进行Decl识别
        int label = 0;

        label:
        while(true){
            grammarId++;
            String type = wordMap.get(grammarId).type;
            switch (type) {
                case "CONSTTK" -> {
                    grammarId--;
                    if (label == 0) {
                        Decl decl = parseDecl();
                        ret.addDecl(decl);
                    }
                }
                case "VOIDTK" -> {
                    label = 1;
                    grammarId--;
                    FuncDef funcDef = parseFuncDef();
                    ret.addFuncDef(funcDef);
                }
                case "INTTK" -> {
                    grammarId++;
                    String type2 = wordMap.get(grammarId).type;
                    if (type2.equals("MAINTK")) {
                        grammarId -= 2; //回溯一次

                        MainFuncDef mainFuncDef = parseMainFuncDef();
                        ret.setMainFuncDef(mainFuncDef);
                        //(ret->mainFuncDef).father = ret;
                        break label;
                    } else if (type2.equals("IDENFR")) {
                        grammarId++;
                        String type3 = wordMap.get(grammarId).type;
                        if (type3.equals("LPARENT")) {
                            grammarId -= 3;
                            label = 1;
                            FuncDef funcDef = parseFuncDef();
                            ret.addFuncDef(funcDef);
                        } else {
                            grammarId -= 3;
                            if (label == 0) {
                                Decl decl = parseDecl();
                                ret.addDecl(decl);
                            }
                        }
                    }
                }
            }
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    // Decl → ConstDecl | VarDecl
    Decl parseDecl(){
        Decl ret = null;
        grammarId++;
        String type = wordMap.get(grammarId).type;
        if(type.equals("CONSTTK")){
            grammarId--;
            ConstDecl constdecl = parseConstDecl();
            ret = constdecl;
        }else if(type.equals("INTTK")){
            grammarId--;
            VarDecl varDecl = parseVarDecl();
            ret = varDecl;
        } else {
            errors.add(new CError(ErrorType.NO_SUCH_DECL, "Invalid type " + type + " found."));
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    FuncDef parseFuncDef(){
        FuncDef ret = new FuncDef();
        Ident ident = new Ident("PROC", wordMap.get(grammarId+1).content);
        grammarId++;
        FuncType funcType = parseFuncType();
        ret.setFuncType(funcType);
        grammarId++;
        LexicalParser.Word word = wordMap.get(grammarId);
        if (word.type.equals("IDENFR")) {
            ident.setIdent(word.content, word.line);
            id2Object.put(treeId++, ident);
            ret.setIdent(ident);
            grammarId++;
            if (wordMap.get(grammarId).type.equals("LPARENT")) {
                grammarId++;
                if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                    grammarId--;
                    FuncFParams funcFParams = parseFuncFParams();
                    ret.setFuncFParams(funcFParams);
                    grammarId++;
                    if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                        grammarId--;
                        //error
                    }
                } else {
                }
                Block block = parseBlock();
                ret.setBlock(block);
            }
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    Block parseBlock(){
        Block ret = new Block();

        grammarId++;
        if (wordMap.get(grammarId).type.equals("LBRACE")) {
            grammarId++;
            while (!wordMap.get(grammarId).type.equals("RBRACE")) {
                grammarId--;
                BlockItem blockItem = parseBlockItem();
                ret.addBlockItem(blockItem);
                grammarId++;
            }
        }
        id2Object.put(treeId++, ret);
        return ret;
    }

    //BlockItem → Decl | Stmt
    BlockItem parseBlockItem(){
        BlockItem ret = new BlockItem();
        grammarId++;
        String name = wordMap.get(grammarId).type;
        if (name.equals("CONSTTK") || name.equals("INTTK")) {
            grammarId--;
            Decl decl = parseDecl();
            ret.setBlockItem(decl); //需要退回一个
        } else {
            grammarId--;
            Stmt stmt = parseStmt();
            ret.setBlockItem(stmt);
        }
        id2Object.put(treeId++, ret);
        return ret;
    }

    /*Stmt → LVal '=' Exp ';'
                    | [Exp] ';' //有?Exp两种情况
                    | Block
                    | 'if' '( Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.?else
                    | 'while' '(' Cond ')' Stmt
                    | 'break' ';' | 'continue' ';'
                    | 'return' [Exp] ';' // 1.有Exp 2.?Exp
                    | LVal = 'getint''('')'';'
                    | 'printf''('FormatString{,Exp}')'';' // 1.有Exp 2.?Exp
    */
    Stmt parseStmt(){
        Stmt ret = new Stmt();
        grammarId++;
        String type = wordMap.get(grammarId).type;
        switch (type) {
            case "IFTK" -> {
                IfStmt ifStmt = new IfStmt();
                grammarId++;
                if (wordMap.get(grammarId).type.equals("LPARENT")) {
                    Cond cond = parseCond();
                    ifStmt.setCond(cond);
                    grammarId++;
                    if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                        grammarId--;
                        //error
                    }
                    Stmt stmt = parseStmt();
                    ifStmt.setStmt(stmt);
                    grammarId++;
                    if (wordMap.get(grammarId).type.equals("ELSETK")) {
                        ElseStmt elseStmt = new ElseStmt();
                        stmt = parseStmt();
                        elseStmt.setStmt(stmt);
                        ifStmt.setElseStmt(elseStmt);
                        id2Object.put(treeId++, elseStmt);
                    } else {
                        grammarId--;
                    }
                }
                id2Object.put(treeId++, ifStmt);
                ret.setStmt(ifStmt);
            }
            case "WHILETK" -> {
                WhileStmt whileStmt = new WhileStmt();
                grammarId++;
                if (wordMap.get(grammarId).type.equals("LPARENT")) {
                    Cond cond = parseCond();
                    whileStmt.setCond(cond);
                    grammarId++;
                    if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                        grammarId--;
                        //error
                    }
                    Stmt stmt = parseStmt();
                    whileStmt.setStmt(stmt);
                }
                id2Object.put(treeId++, whileStmt);
                ret.setStmt(whileStmt);
            }
            case "BREAKTK" -> {
                BreakStmt breakStmt = new BreakStmt();
                grammarId++;
                if (!wordMap.get(grammarId).type.equals("SEMICN")) {
                    grammarId--;
                    //error
                }
                id2Object.put(treeId++, breakStmt);
                ret.setStmt(breakStmt);
            }
            case "CONTINUETK" -> {
                ContinueStmt continueStmt = new ContinueStmt();
                grammarId++;
                if (!wordMap.get(grammarId).type.equals("SEMICN")) {
                    grammarId--;
                    //error
                }
                id2Object.put(treeId++, continueStmt);
                ret.setStmt(continueStmt);
            }
            case "RETURNTK" -> {
                ReturnStmt returnStmt = new ReturnStmt();
                grammarId++;
                if (!wordMap.get(grammarId).type.equals("SEMICN")) {
                    grammarId--;
                    Exp exp = parseExp();
                    returnStmt.setExp(exp);
                    grammarId++;
                    if (!wordMap.get(grammarId).type.equals("SEMICN")) {
                        grammarId--;
                        //error
                    }
                } else {

                }
                id2Object.put(treeId++, returnStmt);
                ret.setStmt(returnStmt);
            }
            case "PRINTFTK" -> {
                PrintStmt printStmt = new PrintStmt();
                grammarId++;
                if (wordMap.get(grammarId).type.equals("LPARENT")) {
                    grammarId++;
                    if (wordMap.get(grammarId).type.equals("STRCON")) { //格式化字符串
                        String formatString = wordMap.get(grammarId).content;
                        printStmt.setFormatString(formatString);
                        grammarId++;
                        while (wordMap.get(grammarId).type.equals("COMMA")) {
                            Exp exp = parseExp();
                            printStmt.addExp(exp);
                            grammarId++;
                        }
                        if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                            grammarId--;
                            //error
                        }
                        grammarId++;
                        if (!wordMap.get(grammarId).type.equals("SEMICN")) {
                            grammarId--;
                            //error
                        }
                    }
                }
                id2Object.put(treeId++, printStmt);
                ret.setStmt(printStmt);
            }
            case "IDENFR" -> {
                grammarId--;
                if (checkEqualSign(grammarId + 1)) {
                    LVal lVal = parseLVal();
                    grammarId++;
                    if (wordMap.get(grammarId).content.equals("=")) {
                        grammarId++;
                        if (wordMap.get(grammarId).content.equals("getint")) {
                            LValGetint lValGetint = new LValGetint();
                            lValGetint.setLVal(lVal);
                            grammarId++;
                            grammarId++;
                            if (!wordMap.get(grammarId).content.equals(")")) {
                                grammarId--;
                                //errpr
                            }
                            grammarId++;
                            if (!wordMap.get(grammarId).content.equals(";")) {
                                grammarId--;
                                //error
                            }
                            id2Object.put(treeId++, lValGetint);
                            ret.setStmt(lValGetint);
                        } else {
                            grammarId--;
                            LValDecl lValDecl = new LValDecl();
                            lValDecl.setLVal(lVal);
                            Exp exp = parseExp();
                            lValDecl.setExp(exp);
                            id2Object.put(treeId++, lValDecl);
                            ret.setStmt(lValDecl);
                            grammarId++;
                            if (!wordMap.get(grammarId).content.equals(";")) {
                                grammarId--;
                                //error
                            }
                        }
                    } else {
                        grammarId--;
                        grammarId--;
                        ExpStmt expStmt = new ExpStmt();
                        Exp exp = parseExp();
                        expStmt.setExp(exp);
                        ret.setStmt(expStmt);
                        grammarId++;
                        if (!wordMap.get(grammarId).content.equals(";")) {
                            grammarId--;
                            //error
                        }
                    }
                } else {
                    ExpStmt expStmt = new ExpStmt();
                    Exp exp = parseExp();
                    expStmt.setExp(exp);
                    ret.setStmt(expStmt);
                    grammarId++;
                    if (!wordMap.get(grammarId).content.equals(";")) {
                        grammarId--;
                        //error
                    }
                }
            }
            case "LBRACE" -> {
                grammarId--;
                Block block = parseBlock();
                ret.setStmt(block);
            }
            case "SEMICN" -> {
                ExpStmt expStmt = new ExpStmt();
                ret.setStmt(expStmt);
            }
            default -> {
                grammarId--;
                ExpStmt expStmt = new ExpStmt();
                Exp exp = parseExp();
                expStmt.setExp(exp);
                ret.setStmt(expStmt);
                grammarId++;
                if (!wordMap.get(grammarId).content.equals("SEMICN")) {
                    grammarId--;
                    //error
                }
            }
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    Boolean checkEqualSign(int a) {
        while (!wordMap.get(a).content.equals(";")) {
            if (wordMap.get(a).content.equals("=")) {
                return true;
            }
            a++;
        }
        return false;
    }

    LVal parseLVal(){
        LVal ret = new LVal();
        Ident ident = new Ident();
        grammarId++;
        LexicalParser.Word word = wordMap.get(grammarId);
        if (word.type.equals("IDENFR")) {
            ident.setIdent(word.content, word.line);
            grammarId++;
            int count = 0;
            while (wordMap.get(grammarId).content.equals("[")) {
                Exp exp = parseExp();
                ret.addExp(exp);
                grammarId++;
                if (!wordMap.get(grammarId).content.equals("]")) {
                    grammarId--;
                    //error
                }
                count++;
                grammarId++;
            }
            ident.setDimension(count);
            id2Object.put(treeId++, ident);
        }
        ret.setIdent(ident);
        grammarId--;

        id2Object.put(treeId++, ret);
        return ret;
    }

    //Cond → LOrExp
    Cond parseCond(){
        Cond ret = new Cond();
        LOrExp lOrExp = parseLOrExp();
        ret.setLOrExp(lOrExp);
        id2Object.put(treeId++, ret);
        return ret;
    }

    //FuncFParams → FuncFParam { ',' FuncFParam }
    FuncFParams parseFuncFParams(){
        FuncFParams ret = new FuncFParams();
        FuncFParam funcFParam = parseFuncFParam();
        ret.addFuncFParam(funcFParam);
        grammarId++;
        while (wordMap.get(grammarId).type.equals("COMMA")) {
            funcFParam = parseFuncFParam();
            ret.addFuncFParam(funcFParam);
            grammarId++;
        }
        grammarId--;
        id2Object.put(treeId++, ret);
        return ret;
    }

    FuncFParam parseFuncFParam(){
        FuncFParam ret = new FuncFParam();

        Ident ident = new Ident("PARA","INT");
        grammarId++;
        parseByte();
        grammarId++;

        LexicalParser.Word word = wordMap.get(grammarId);
        if (word.type.equals("IDENFR")) {
            ident.setIdent(word.content, word.line);
            grammarId++;
            int count = 0;
            if (wordMap.get(grammarId).type.equals("LBRACK")) {
                grammarId++;
                if (wordMap.get(grammarId).type.equals("RBRACK")) {
                    count++;
                    grammarId++;
                    ret.setType(FuncParamType.ARRAY1D);
                    while (wordMap.get(grammarId).type.equals("LBRACK")) {
                        ConstExp constExp = parseConstExp();
                        ret.addConstExp(constExp);
                        ret.setType(FuncParamType.ARRAYMULTID);
                        grammarId++;
                        if (!wordMap.get(grammarId).type.equals("RBRACK")) {
                            grammarId--;
                            //error
                        }
                        count++;
                        grammarId++;
                    }
                    grammarId--;
                } else {
                    grammarId--;
                    //error
                }
            } else {
                grammarId--;
            }
            ident.setDimension(count);
            id2Object.put(treeId++, ident);
            ret.setIdent(ident);
        }
    
        id2Object.put(treeId++, ret);

        return ret;
    }

    FuncType parseFuncType(){
        FuncType ret = new FuncType();
        String name = wordMap.get(grammarId).type;
        if(name.equals("VOIDTK") || name.equals("INTTK")){
            
        }
        ret.setName(name);
        
        id2Object.put(treeId++, ret);

        return ret;
    }

    //MainFuncDef → 'int' 'main' '(' ')' Block
    MainFuncDef parseMainFuncDef(){
        MainFuncDef ret = new MainFuncDef();

        grammarId++;
        if (wordMap.get(grammarId).type.equals("INTTK")) {
            grammarId++;
            if (wordMap.get(grammarId).type.equals("MAINTK")) {
                grammarId++;
                if (wordMap.get(grammarId).type.equals("LPARENT")) {
                    grammarId++;
                    if (!wordMap.get(grammarId).type.equals("RPARENT")) {
                        grammarId--;
                        //error
                    } else {

                    }
                    Block block = parseBlock();
                    ret.setBlock(block);
                }
            }
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    ConstDecl parseConstDecl(){
        ConstDecl ret = new ConstDecl();
        grammarId++;
        String type = wordMap.get(grammarId).type;
        if(type.equals("CONSTTK")){
            grammarId++;
            parseByte();
            ConstDef constDef = parseConstDef();
            ret.addconstDef(constDef);
            grammarId++;
            while(wordMap.get(grammarId).type.equals("COMMA")){
                constDef = parseConstDef();
                ret.addconstDef(constDef);
                grammarId++;
            }
            if(!wordMap.get(grammarId).type.equals("SEMICN")){
                grammarId--;
                // error
            }
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    ConstDef parseConstDef(){
        ConstDef ret = new ConstDef();
        Ident ident = new Ident("VAR", "CONST");

        grammarId++;
        LexicalParser.Word word = wordMap.get(grammarId);
        if(word.type.equals("IDENFR")){
            ident.setIdent(word.content, word.line);
        }
        grammarId++;
        //
        int count = 0;
        while(wordMap.get(grammarId).type.equals("LBRACK")){
            ConstExp constExp = parseConstExp();
            ret.addConstExp(constExp);
            grammarId++;
            if(!wordMap.get(grammarId).type.equals("RBRACK")){
                grammarId--;
                errors.add(new CError(ErrorType.EXPECT_TOKEN, "Expecet ]."));
            }
            count++;
            grammarId++;
        }
        ident.setDimension(count);

        if(wordMap.get(grammarId).type.equals("ASSIGN")){
            ConstInitVal constInitVal = parseConstInitVal();
            ret.setConstInitVal(constInitVal);
        }

        ret.setIdent(ident);
        id2Object.put(treeId++, ident);

        return ret;
    }

    void parseByte(){
        String type = wordMap.get(grammarId).content;
        if(type.equals("INTTK")){

        }
    }

    ConstExp parseConstExp(){
        ConstExp ret = new ConstExp();
        AddExp addExp = parseAddExp();
        ret.setAddExp(addExp);
        id2Object.put(treeId++, ret);
        return ret;
    }

    //ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}' 
    ConstInitVal parseConstInitVal(){
        ConstInitVal ret = new ConstInitVal();
        ConstInitValList tmp = new ConstInitValList();
        grammarId++;
        if(wordMap.get(grammarId).type.equals("LBRACE")){
            grammarId++;
            if(!wordMap.get(grammarId).type.equals("RBRACE")){
                grammarId--;
                ConstInitVal constInitVal = parseConstInitVal();
                tmp.addConstInitVal(constInitVal);
                grammarId++;
                while (wordMap.get(grammarId).type.equals("COMMA")) {
                    constInitVal = parseConstInitVal();
                    tmp.addConstInitVal(constInitVal);
                    grammarId++;
                }   
            }
            id2Object.put(treeId++, tmp);
            ret.setConstForm(tmp);
        }else{
            grammarId--;
            ConstExp constExp = parseConstExp();
            ret.setConstForm(constExp);
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //VarDecl → BType VarDef { ',' VarDef } ';'
    VarDecl parseVarDecl(){
        VarDecl ret = new VarDecl();
        grammarId++;
        parseByte();

        VarDef varDef = parseVarDef();
        ret.addVarDef(varDef);
        grammarId++;

        while (wordMap.get(grammarId).type.equals("COMMA")) {
            varDef = parseVarDef();
            ret.addVarDef(varDef);
            grammarId++;
        }
        if (wordMap.get(grammarId).type.equals("SEMICN")) {

        } else {
            grammarId--;
            //error
        }
    
        id2Object.put(treeId++, ret);

        return ret;
    }

    //VarDef → Ident { '[' ConstExp ']'} | Ident { '[' ConstExp ']' } '=' InitVal
    VarDef parseVarDef(){
        VarDef ret = new VarDef();
        Ident ident = new Ident("VAR","INT");
        grammarId++;

        LexicalParser.Word word = wordMap.get(grammarId);
        if (word.type.equals("IDENFR")) {
            ident.setIdent(word.content, word.line);
            grammarId++;
            int count = 0;
            while (wordMap.get(grammarId).type.equals("LBRACK")) {
                ConstExp constExp = parseConstExp();
                ret.addConstExp(constExp);
                grammarId++;
                if (!wordMap.get(grammarId).type.equals("RBRACK")) {
                    grammarId--;
                    //error
                }
                count++;
                grammarId++;
            }
            ident.setDimension(count);
            id2Object.put(treeId++, ident);
            ret.setIdent(ident);
            grammarId--;
        }
        grammarId++;
        if (wordMap.get(grammarId).type.equals("ASSIGN")) {
            InitVal initVal = parseInitVal();
            ret.setInitVal(initVal);
        } else {
            //无初值
            grammarId--;
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    InitVal parseInitVal(){
        InitVal ret = new InitVal();
        InitValList tmp = new InitValList();
        grammarId++;
        if (wordMap.get(grammarId).type.equals("LBRACE")) {
            grammarId++;
            if (!wordMap.get(grammarId).type.equals("RBRACE")) {
                grammarId--;
                InitVal initVal = parseInitVal();
                tmp.addInitVal(initVal);
                grammarId++;
                while (wordMap.get(grammarId).type.equals("COMMA")) {
                    initVal = parseInitVal();
                    tmp.addInitVal(initVal);
                    grammarId++;
                } //下一个一定是}
                ret.setInitForm(tmp);
                id2Object.put(treeId++, tmp);
            } else {
               
            }
        } else {
            grammarId--;
            Exp exp = parseExp();
            ret.setInitForm(exp);
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    Exp parseExp(){
        Exp ret = new Exp();
        AddExp addExp = parseAddExp();
        ret.setAddExp(addExp);
        id2Object.put(treeId++, ret);
        return ret;
    }

    AddExp parseAddExp(){
        AddExp ret = new AddExp();
        MulExp mulExp = parseMulExp();
        Queue<MulExp> mulQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        mulQueue.add(mulExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("+") || name.equals("-")) {
            flag = 1;
            opQueue.add(name);
            mulExp = parseMulExp();
            mulQueue.add(mulExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个mulExp
            grammarId--;
            ret.setMulExp(mulExp);
            return ret;
        }

        AddExp addExp = new AddExp();
        addExp.setMulExp(mulQueue.peek());
        mulQueue.poll(); //第一个元素
        while (!mulQueue.isEmpty()){
            AddExp addExp2 = new AddExp();
            addExp2.setAddExp(addExp);
            addExp2.setCh(opQueue.peek());
            opQueue.poll();
            addExp2.setMulExp(mulQueue.peek());
            mulQueue.poll();
            addExp = addExp2;
            ret = addExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);

        return ret;
    }

    //MulExp → UnaryExp | MulExp ('*' | '/' | '%')
    MulExp parseMulExp(){
        MulExp ret = new MulExp();
        UnaryExp unaryExp = parseUnaryExp();
        Queue<UnaryExp> unaryQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        unaryQueue.add(unaryExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("*") || name.equals("/") || name.equals("%")) {
            flag = 1;
            opQueue.add(name);
            unaryExp = parseUnaryExp();
            unaryQueue.add(unaryExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个mulExp
            grammarId--;
            ret.setUnaryExp(unaryExp);
            return ret;
        }

        MulExp mulExp = new MulExp();
        mulExp.setUnaryExp(unaryQueue.peek());
        unaryQueue.poll(); //第一个元素
        while (!unaryQueue.isEmpty()){
            MulExp mulExp2 = new MulExp();
            mulExp2.setMulExp(mulExp);
            mulExp2.setCh(opQueue.peek());
            opQueue.poll();
            mulExp2.setUnaryExp(unaryQueue.peek());
            unaryQueue.poll();
            mulExp = mulExp2;
            ret = mulExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);
        return ret;
    }

    LOrExp parseLOrExp(){
        LOrExp ret = new LOrExp();
        LAndExp lAndExp = parseLAndExp();
        Queue<LAndExp> lAndQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        lAndQueue.add(lAndExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("||" )) {
            flag = 1;
            opQueue.add(name);
            lAndExp = parseLAndExp();
            lAndQueue.add(lAndExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个lOrExp
            grammarId--;
            ret.setLAndExp(lAndExp);
            return ret;
        }

        LOrExp lOrExp = new LOrExp();
        lOrExp.setLAndExp(lAndQueue.peek());
        lAndQueue.poll(); //第一个元素
        while (!lAndQueue.isEmpty()){
            LOrExp lOrExp2 = new LOrExp();
            lOrExp2.setLOrExp(lOrExp);
            lOrExp2.setCh(opQueue.peek());
            opQueue.poll();
            lOrExp2.setLAndExp(lAndQueue.peek());
            lAndQueue.poll();
            lOrExp = lOrExp2;
            ret = lOrExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);

        return ret;
    }

    LAndExp parseLAndExp(){
        LAndExp ret = new LAndExp();
        EqExp eqExp = parseEqExp();
        Queue<EqExp> eqQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        eqQueue.add(eqExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("&&")) {
            flag = 1;
            opQueue.add(name);
            eqExp = parseEqExp();
            eqQueue.add(eqExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个lAndExp
            grammarId--;
            ret.setEqExp(eqExp);
            return ret;
        }

        LAndExp lAndExp = new LAndExp();
        lAndExp.setEqExp(eqQueue.peek());
        eqQueue.poll(); //第一个元素
        while (!eqQueue.isEmpty()){
            LAndExp lAndExp2 = new LAndExp();
            lAndExp2.setLAndExp(lAndExp);
            lAndExp2.setCh(opQueue.peek());
            opQueue.poll();
            lAndExp2.setEqExp(eqQueue.peek());
            eqQueue.poll();
            lAndExp = lAndExp2;
            ret = lAndExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);

        return ret;
    }

    EqExp parseEqExp(){
        EqExp ret = new EqExp();
        RelExp relExp = parseRelExp();
        Queue<RelExp> relQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        relQueue.add(relExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("==") || name.equals("!=")) {
            flag = 1;
            opQueue.add(name);
            relExp = parseRelExp();
            relQueue.add(relExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个eqExp
            grammarId--;
            ret.setRelExp(relExp);
            return ret;
        }

        EqExp eqExp = new EqExp();
        eqExp.setRelExp(relQueue.peek());
        relQueue.poll(); //第一个元素
        while (!relQueue.isEmpty()){
            EqExp eqExp2 = new EqExp();
            eqExp2.setEqExp(eqExp);
            eqExp2.setCh(opQueue.peek());
            opQueue.poll();
            eqExp2.setRelExp(relQueue.peek());
            relQueue.poll();
            eqExp = eqExp2;
            ret = eqExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);

        return ret;
    }

    RelExp parseRelExp(){
        RelExp ret = new RelExp();
        AddExp addExp = parseAddExp();
        Queue<AddExp> addQueue = new LinkedList<>();
        Queue<String> opQueue = new LinkedList<>();
        addQueue.add(addExp);
        grammarId++;
        String name;
        int flag = 0;

        while ((name = wordMap.get(grammarId).content).equals("<") || name.equals(">") || name.equals("<=") || name.equals(">=")) {
            flag = 1;
            opQueue.add(name);
            addExp = parseAddExp();
            addQueue.add(addExp);
            grammarId++;
        }
        
        if(flag == 0){ //只有一个relExp
            grammarId--;
            ret.setAddExp(addExp);
            return ret;
        }

        RelExp relExp = new RelExp();
        relExp.setAddExp(addQueue.peek());
        addQueue.poll(); //第一个元素
        while (!addQueue.isEmpty()){
            RelExp relExp2 = new RelExp();
            relExp2.setRelExp(relExp);
            relExp2.setCh(opQueue.peek());
            opQueue.poll();
            relExp2.setAddExp(addQueue.peek());
            addQueue.poll();
            relExp = relExp2;
            ret = relExp2;
        }
            
        grammarId--;
        id2Object.put(treeId++, ret);

        return ret;
    }

    //PrimaryExp → '(' Exp ')' | LVal | Number
    PrimaryExp parsePrimaryExp(){
        PrimaryExp ret = new PrimaryExp();

        grammarId++;
        String name = wordMap.get(grammarId).content;
        String type = wordMap.get(grammarId).type;
        if (name.equals("(")) {
            Exp exp = parseExp();
            ret.setWrappedExp(exp);
            grammarId++;
            if (!wordMap.get(grammarId).content.equals(")")) {
                grammarId--;
                //error
            }
        } else if (type.equals("INTCON")) {
            GNumber number = parseNumber();
            ret.setWrappedExp(number);
        } else {
            grammarId--;
            LVal lVal = parseLVal();
            ret.setWrappedExp(lVal);
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    UnaryExp parseUnaryExp(){
        UnaryExp ret = new UnaryExp();
        grammarId++;
        LexicalParser.Word word = wordMap.get(grammarId);
        if (word.type.equals("IDENFR")) {
            Ident ident = new Ident();
            ident.setIdent(word.content, word.line);
            grammarId++;
            if (wordMap.get(grammarId).content.equals("(")) {
                FuncCall funcCall = new FuncCall();
                funcCall.setIdent(ident);
                grammarId++;
                if (!wordMap.get(grammarId).content.equals(")")) { //如果这里是x(中间没有参数表，但是缺少)怎么办 TODO
                    grammarId--;
                    FuncRParams funcRParams = parseFuncRParams();
                    funcCall.setFuncRParams(funcRParams);
                    grammarId++;
                    if (!wordMap.get(grammarId).content.equals(")")) {
                        grammarId--;
                        //error
                    }
                } else {

                }
                id2Object.put(treeId++, funcCall);
                ret.setWrappedExp(funcCall);
            } else {
                grammarId--;
                grammarId--;
                PrimaryExp primaryExp = parsePrimaryExp();
                ret.setWrappedExp(primaryExp);
            }
        } else if ((word.content.equals("+") || word.content.equals("-") || (word.content.equals("!")))) {
            OpExp opExp = new OpExp();
            UnaryOp unaryOp = parseUnaryOp();
            opExp.setUnaryOp(unaryOp);
            UnaryExp unaryExp = parseUnaryExp();
            opExp.setUnaryExp(unaryExp);
            id2Object.put(treeId++, opExp);
            ret.setWrappedExp(unaryExp);
        } else {
            grammarId--;
            PrimaryExp primaryExp = parsePrimaryExp();
            ret.setWrappedExp(primaryExp);
        }

        id2Object.put(treeId++, ret);

        return ret;
    }

    GNumber parseNumber(){
        GNumber ret = new GNumber();
        ret.setNumber(Integer.parseInt(wordMap.get(grammarId).content));
        id2Object.put(treeId++, ret);
        return ret;
    }

    FuncRParams parseFuncRParams(){
        FuncRParams ret = new FuncRParams();
        Exp exp = parseExp();
        ret.addExp(exp);
        grammarId++;
        while (wordMap.get(grammarId).content.equals(",")) {
            exp = parseExp();
            ret.addExp(exp);
            grammarId++;
        }
        grammarId--;
        id2Object.put(treeId++, ret);
        return ret;
    }

    UnaryOp parseUnaryOp(){
        UnaryOp ret = new UnaryOp();
        ret.setUnaryOp(wordMap.get(grammarId).content);
        id2Object.put(treeId++, ret);
        return ret;
    }





}

