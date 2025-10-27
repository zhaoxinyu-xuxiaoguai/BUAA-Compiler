package error;

import node.*;
//import symbol.*;
import smybol.*;
import utils.IOUtils;
import utils.Quadruple;


import java.io.IOException;

import java.util.*;

public class ErrorHandler {
    private static final ErrorHandler instance = new ErrorHandler();

    public static ErrorHandler getInstance() {
        return instance;
    }


    private List<Error> errors = new ArrayList<>();
    private List<Quadruple<LinkedHashMap<String, Symbol>,Boolean, FuncType,Integer>> symbolTables=new ArrayList<>();
    private List<Quadruple<LinkedHashMap<String, Symbol>,Boolean, FuncType,Integer>> printSymbolTables=new ArrayList<>();

    private void addSymbolTable(boolean isFunc,FuncType funcType){
        symbolTables.add(new Quadruple<>(new LinkedHashMap<>(), isFunc,funcType,getTotalArea()+1));
        //printSymbolTables.add(new Quadruple<>(new LinkedHashMap<>(), isFunc,funcType,getTotalArea()+1));
    }
    private int totalArea=0;


    //分清楚当前域的总数和域的总数（包括已经弹栈出去的）
    private int getCurrentArea(){
        return symbolTables.size();
    }
    private int getTotalArea(){
        return totalArea;
    }

    private void removeSymbolTable(){
        printSymbolTables.add(symbolTables.get(symbolTables.size()-1));
        symbolTables.remove(symbolTables.size()-1);
    }

    private boolean containsInCurrent(String ident){
        return symbolTables.get(symbolTables.size()-1).getFirst().containsKey(ident);
    }

    private boolean contains(String ident){
        for (int i = symbolTables.size()-1; i >=0 ; i--) {
            if(symbolTables.get(i).getFirst().containsKey(ident)){
                return true;
            }
        }
        return false;
    }

    private boolean isInFunc(){
        for (int i = symbolTables.size()-1; i >=0 ; i--) {
            if(symbolTables.get(i).getSecond()){
                return true;
            }
        }
        return false;
    }

    private FuncType getFuncType(){
        for (int i = symbolTables.size()-1; i >=0 ; i--) {
            if(symbolTables.get(i).getSecond()){
                return symbolTables.get(i).getThird();
            }
        }
        return null;
    }

    private boolean isCurrentFunc(){
        return symbolTables.get(symbolTables.size()-1).getSecond();
    }

    private FuncType getCurrentFuncType(){
        return symbolTables.get(symbolTables.size()-1).getThird();
    }

    private void put(String ident,Symbol symbol){
        symbolTables.get(symbolTables.size()-1).getFirst().put(ident,symbol);
        //printSymbolTables.get(printSymbolTables.size()-1).getFirst().put(ident,symbol);
    }




    private Symbol get(String ident){
        for (int i = symbolTables.size()-1; i >=0 ; i--) {
            if(symbolTables.get(i).getFirst().containsKey(ident)){
                return symbolTables.get(i).getFirst().get(ident);
            }
        }
        return null;
    }

    public static int loopCount=0;
    public List<Error> getErrors() {
        return errors;
    }

    public void printErrors() throws IOException {
        errors.sort(Error::compareTo);
        for (Error error : errors) {
            IOUtils.error(error.toString());
        }
    }

    public void printSymbolTablesFun() throws IOException{
        List<Quadruple<LinkedHashMap<String, Symbol>, Boolean, FuncType, Integer>> sortedTables =
                new ArrayList<>(printSymbolTables);

        sortedTables.sort(new Comparator<Quadruple<LinkedHashMap<String, Symbol>, Boolean, FuncType, Integer>>() {
            @Override
            public int compare(Quadruple<LinkedHashMap<String, Symbol>, Boolean, FuncType, Integer> o1,
                               Quadruple<LinkedHashMap<String, Symbol>, Boolean, FuncType, Integer> o2) {
                return Integer.compare(o1.getFourth(), o2.getFourth());
            }
        });

        for (Quadruple<LinkedHashMap<String, Symbol>, Boolean, FuncType, Integer> printSymbolTable : sortedTables) {
            for (Map.Entry<String, Symbol> entry : printSymbolTable.getFirst().entrySet()) {
                if (Objects.equals(entry.getKey(), "main")||Objects.equals(entry.getKey(),"getint")) {
                    continue;
                } else {
                    IOUtils.symbol(printSymbolTable.getFourth() + " " + entry.getKey() + " " + entry.getValue().getSymbolType()+"\n");
                    //System.out.println(printSymbolTable.getFourth() + " " + entry.getKey() + " " + entry.getValue().getSymbolType());
                }
            }
        }
    }

    public void addError(Error newError) {
        for (Error error : errors) {
            if (error.equals(newError)) {
                return;
            }
        }
       
        errors.add(newError);
    }





    public void compUnitError(CompUnitNode compUnitNode) {
        //CompUnit → {Decl} {FuncDef} MainFuncDef
        addSymbolTable(false,null);
        totalArea+=1;
        FuncSymbol getintSymbol=new FuncSymbol("getint", "getint",FuncType.INT, new ArrayList<>());
        put("getint",getintSymbol);

        for (DeclNode decl : compUnitNode.getDeclNodes()){
            declError(decl);
        }
        for(FuncDefNode funcDef: compUnitNode.getFuncDefNodes()){
            funcDefError(funcDef);
        }
        mainFuncDefError(compUnitNode.getMainFuncDefNode());
        removeSymbolTable();
    }

    private void declError(DeclNode declNode) {
        // Decl -> ConstDecl | VarDecl
        if(declNode.getConstDecl()!=null){
            constDeclError(declNode.getConstDecl());
        }else{
            varDeclError(declNode.getVarDecl());
        }
    }

    private void constDeclError(ConstDeclNode constDeclNode) {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        for(ConstDefNode constDef:constDeclNode.getConstDefNodeList()){
            constDefError(constDef);
        }
    }

    private void constDefError(ConstDefNode constDefNode) {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        if(containsInCurrent(constDefNode.getIdent().getContent())){
            ErrorHandler.getInstance().addError(new Error(constDefNode.getIdent().getLineNumber(),ErrorType.b));
        }
        if(!constDefNode.getConstExpNodes().isEmpty()){
            for (ConstExpNode constExpNode :constDefNode.getConstExpNodes()){
                constExpError(constExpNode);
            }
        }
        if(constDefNode.getConstExpNodes().isEmpty()){
            put(constDefNode.getIdent().getContent(),new ArraySymbol(constDefNode.getIdent().getContent(),"ConstInt",true,0));
        }else{
            put(constDefNode.getIdent().getContent(),new ArraySymbol(constDefNode.getIdent().getContent(),"ConstIntArray",true,1));
        }

        constInitValError(constDefNode.getConstInitValNode());

    }

    private void constInitValError(ConstInitValNode constInitValNode) {
        // ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
        for(ConstExpNode constExpNode:constInitValNode.getConstExpNode()){
            constExpError(constExpNode);
        }
    }

    private void varDeclError(VarDeclNode varDeclNode) {
        // VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
        boolean flag=varDeclNode.getStaticToken()!=null;
        for(VarDefNode varDefNode:varDeclNode.getVarDefNodes()){
            varDefError(flag,varDefNode);
        }
    }

    private void varDefError(boolean isStatic,VarDefNode varDefNode) {
        // VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
        if(containsInCurrent(varDefNode.getIdent().getContent())){
            ErrorHandler.getInstance().addError(new Error(varDefNode.getIdent().getLineNumber(),ErrorType.b));
        }
        if(varDefNode.getConstExpNodes()!=null){
            constExpError(varDefNode.getConstExpNodes());
        }

        String symbolType;
        int dimension=varDefNode.getConstExpNodes()==null?0:1;
        if(isStatic){
            if(dimension==0)symbolType="StaticInt";
            else symbolType="StaticIntArray";
        }else{
            if(dimension==1)symbolType="IntArray";
            else symbolType="Int";
        }
        put(varDefNode.getIdent().getContent(),new ArraySymbol(varDefNode.getIdent().getContent(),symbolType, false,dimension ));
        if(varDefNode.getInitValNode()!=null){
            initValError(varDefNode.getInitValNode());
        }
    }

    private void initValError(InitValNode initValNode) {
        // InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
            for (ExpNode expNode : initValNode.getExpNode()) {
                expError(expNode);
            }
    }

    private void funcDefError(FuncDefNode funcDefNode) {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        if(containsInCurrent(funcDefNode.getIdent().getContent())){
            ErrorHandler.getInstance().addError(new Error(funcDefNode.getIdent().getLineNumber(),ErrorType.b));
        }

        String symbolType=funcDefNode.getFuncTypeNode().getType()==FuncType.VOID?"VoidFunc":"IntFunc";
        if(funcDefNode.getFuncFParamsNode()==null){
            put(funcDefNode.getIdent().getContent(),new FuncSymbol(funcDefNode.getIdent().getContent(),symbolType,funcDefNode.getFuncTypeNode().getType(),new ArrayList<>()));
        }else{
            List<FuncParam> params=new ArrayList<>();
            for(FuncFParamNode funcFParamNode:funcDefNode.getFuncFParamsNode().getFuncFParamNodes()){
                params.add(new FuncParam(funcFParamNode.getIdent().getContent(),funcFParamNode.getLeftBrackets()==null?0:1));
            }
            put(funcDefNode.getIdent().getContent(),new FuncSymbol(funcDefNode.getIdent().getContent(),symbolType,funcDefNode.getFuncTypeNode().getType(),params));
        }
        addSymbolTable(true,funcDefNode.getFuncTypeNode().getType());
        totalArea+=1;
        if(funcDefNode.getFuncFParamsNode()!=null){
            funcFParamsError(funcDefNode.getFuncFParamsNode());
        }
        blockError(funcDefNode.getBlockNode());
        removeSymbolTable();
    }

    private void mainFuncDefError(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        put("main",new FuncSymbol("main",null,FuncType.INT,new ArrayList<>()));
        addSymbolTable(true,FuncType.INT);
        totalArea+=1;
        blockError(mainFuncDefNode.getBlockNode());
        removeSymbolTable();
    }

    private void funcFParamsError(FuncFParamsNode funcFParamsNode) {
        // FuncFParams → FuncFParam { ',' FuncFParam }
        for(FuncFParamNode funcFParam:funcFParamsNode.getFuncFParamNodes()){
            funcFParamError(funcFParam);
        }
    }

    private void funcFParamError(FuncFParamNode funcFParamNode) {
        //  FuncFParam → BType Ident ['[' ']']
        if (containsInCurrent(funcFParamNode.getIdent().getContent())) {
            ErrorHandler.getInstance().addError(new Error(funcFParamNode.getIdent().getLineNumber(), ErrorType.b));
        }
        String symbolType=funcFParamNode.getLeftBrackets()==null?"Int":"IntArray";
        int dimension=funcFParamNode.getLeftBrackets()==null?0:1;
        put(funcFParamNode.getIdent().getContent(), new ArraySymbol(funcFParamNode.getIdent().getContent(),symbolType,false,dimension));
    }

    private void blockError(BlockNode blockNode) {
        // Block -> '{' { BlockItem } '}'
        for(BlockItemNode blockItemNode :blockNode.getBlockItemNodes()){
                    blockItemNodeError(blockItemNode);
        }
        if(isCurrentFunc()){
            if(getCurrentFuncType()== FuncType.INT){
                if(blockNode.getBlockItemNodes().isEmpty()||
                        blockNode.getBlockItemNodes().get(blockNode.getBlockItemNodes().size()-1).getStmtNode()==null ||
                        blockNode.getBlockItemNodes().get(blockNode.getBlockItemNodes().size()-1).getStmtNode().getReturnToken()==null){
                    ErrorHandler.getInstance().addError(new Error(blockNode.getRightBraceToken().getLineNumber(),ErrorType.g));

                }
            }
        }
    }

    private void blockItemNodeError(BlockItemNode blockItemNode) {
        // BlockItem -> Decl | Stmt
        if(blockItemNode.getDeclNode()!=null){
            declError(blockItemNode.getDeclNode());
        }else{
            stmtError(blockItemNode.getStmtNode());
        }
    }

    private void stmtError(StmtNode stmtNode) {
        // Stmt -> LVal '=' Exp ';'
        //	| [Exp] ';'
        //	| Block
        //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        //	| 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        //	| 'break' ';' | 'continue' ';'
        //	| 'return' [Exp] ';'
        //	| LVal '=' 'Exp' ';'
        //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
        switch(stmtNode.getType()){
            case Exp:
                //[Exp] ';'
                if(stmtNode.getExpNode()!=null) expError(stmtNode.getExpNode());
                break;
            case Block:
                //Block
                addSymbolTable(false,null);
                totalArea+=1;
                blockError(stmtNode.getBlockNode());
                removeSymbolTable();
                break;
            case If:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                condError(stmtNode.getCondNode());
                stmtError(stmtNode.getStmtNodes().get(0));
                if(stmtNode.getElseToken()!=null){
                    stmtError(stmtNode.getStmtNodes().get(1));
                }
                break;
            case For:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                if(stmtNode.getForStmtNode1()!=null) forStmtError(stmtNode.getForStmtNode1());
                if(stmtNode.getCondNode()!=null)condError(stmtNode.getCondNode());
                if(stmtNode.getForStmtNode2()!=null)forStmtError(stmtNode.getForStmtNode2());
                ErrorHandler.loopCount++;
                stmtError(stmtNode.getStmtNodes().get(0));
                ErrorHandler.loopCount--;
                break;
            case Break:
                // 'break' ';'
            case Continue:
                // 'continue' ';'
                if(ErrorHandler.loopCount==0){
                    ErrorHandler.getInstance().addError(new Error(stmtNode.getBreakOrContinueToken().getLineNumber(),ErrorType.m));
                }
                break;
            case Return:
                // 'return' [Exp] ';'
                if(isInFunc()){
                    if(getFuncType()==FuncType.VOID && stmtNode.getExpNode()!=null){
                        ErrorHandler.getInstance().addError(new Error(stmtNode.getReturnToken().getLineNumber(), ErrorType.f));
                    }
                    if(stmtNode.getExpNode()!=null){
                        expError(stmtNode.getExpNode());
                    }
                }
                break;
            case LValAssignExp:
                //LVal '=' Exp ';'
                lValError(stmtNode.getLValNode());
                if (get(stmtNode.getLValNode().getIdent().getContent()) instanceof ArraySymbol) {
                    ArraySymbol arraySymbol = (ArraySymbol) get(stmtNode.getLValNode().getIdent().getContent());
                    if (arraySymbol.isConst()) {
                        ErrorHandler.getInstance().addError(new Error(stmtNode.getLValNode().getIdent().getLineNumber(), ErrorType.h));
                    }
                }
                expError(stmtNode.getExpNode());
                break;
            case Printf:
                // 'printf' '(' FormatString { ',' Exp } ')' ';'
                int numOfExp=stmtNode.getExpNodes().size();
                int numOfFormatString=0;
                for (int i = 0; i < stmtNode.getFormatString().toString().length(); i++) {
                    if(stmtNode.getFormatString().toString().charAt(i)=='%'){
                        if(stmtNode.getFormatString().toString().charAt(i+1)=='d'){
                            numOfFormatString++;
                        }
                    }
                }
                if(numOfExp!=numOfFormatString){
                    ErrorHandler.getInstance().addError(new Error(stmtNode.getPrintfToken().getLineNumber(),ErrorType.l));
                }

                for(ExpNode expNode:stmtNode.getExpNodes()){
                    expError(expNode);
                }
                break;
        }
    }
    private void forStmtError(ForStmtNode forStmtNode){
        //ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
        List<LValNode> lValNodes=forStmtNode.getlValNode();
        List<ExpNode> expNodes=forStmtNode.getExpNode();
        for (int i = 0; i < forStmtNode.getlValNode().size(); i++) {
            lValError(lValNodes.get(i));
            if (get(lValNodes.get(i).getIdent().getContent()) instanceof ArraySymbol) {
                ArraySymbol arraySymbol = (ArraySymbol) get(lValNodes.get(i).getIdent().getContent());
                if (arraySymbol.isConst()) {
                    ErrorHandler.getInstance().addError(new Error(lValNodes.get(i).getIdent().getLineNumber(), ErrorType.h));
                }
            }
            expError(expNodes.get(i));
        }
    }

    private void expError(ExpNode expNode) {
        // Exp -> AddExp
        addExpError(expNode.getAddExpNode());
    }



    private void condError(CondNode condNode) {
        // Cond -> LOrExp
        lOrExpError(condNode.getLOrExpNode());
    }

    private void lValError(LValNode lValNode) {
        // LVal → Ident ['[' Exp ']']
        if(!contains(lValNode.getIdent().getContent())){
            ErrorHandler.getInstance().addError(new Error(lValNode.getIdent().getLineNumber(), ErrorType.c));

        }
        for(ExpNode expNode:lValNode.getExpNodes()){
            expError(expNode);
        }
    }



    private void primaryExpError(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            expError(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            lValError(primaryExpNode.getLValNode());
        }
    }


    private FuncParam getFuncParamInExp(ExpNode expNode){
        //Exp->AddExp
        return getFuncParamInAddExp(expNode.getAddExpNode());
    }

    private FuncParam getFuncParamInAddExp(AddExpNode addExpNode){
        // AddExp -> MulExp | AddExp ('+' | '-') MulExp
        return getFuncParamInMulExp(addExpNode.getMulExpNode());
    }

    private FuncParam getFuncParamInMulExp(MulExpNode mulExpNode){
        return getFuncParamInUnaryExp(mulExpNode.getUnaryExpNode());
    }

    private FuncParam getFuncParamInUnaryExp(UnaryExpNode unaryExpNode){
        //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if(unaryExpNode.getPrimaryExpNode()!=null){
            return getFuncParamInPrimaryExp(unaryExpNode.getPrimaryExpNode());
        }else if(unaryExpNode.getIdent()!=null){
            return get(unaryExpNode.getIdent().getContent()) instanceof  FuncSymbol? new FuncParam(unaryExpNode.getIdent().getContent(),0):null;
        }else{
            return getFuncParamInUnaryExp(unaryExpNode.getUnaryExpNode());
        }
    }

    private FuncParam getFuncParamInPrimaryExp(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            return getFuncParamInExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            return getFuncParamInLVal(primaryExpNode.getLValNode());
        } else {
            return new FuncParam(null, 0);
        }
    }

    private FuncParam getFuncParamInLVal(LValNode lValNode) {
        return new FuncParam(lValNode.getIdent().getContent(), lValNode.getExpNodes().size());
    }

    private void unaryExpError(UnaryExpNode unaryExpNode) {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if(unaryExpNode.getPrimaryExpNode()!=null){
            primaryExpError(unaryExpNode.getPrimaryExpNode());
        }else if(unaryExpNode.getUnaryExpNode()!=null){
            unaryExpError(unaryExpNode.getUnaryExpNode());
        }else{
            if(!contains(unaryExpNode.getIdent().getContent())){
                ErrorHandler.getInstance().addError(new Error(unaryExpNode.getIdent().getLineNumber(),ErrorType.c));
                return;
            }
            Symbol symbol = get(unaryExpNode.getIdent().getContent());
            FuncSymbol funcSymbol =(FuncSymbol)symbol;
            if(unaryExpNode.getFuncRParamsNode()==null){
                if(!funcSymbol.getFuncParams().isEmpty()){
                    ErrorHandler.getInstance().addError(new Error(unaryExpNode.getIdent().getLineNumber(),ErrorType.d));
                }
            }else{
                if(funcSymbol.getFuncParams().size()!=unaryExpNode.getFuncRParamsNode().getExpNodes().size()){
                    ErrorHandler.getInstance().addError(new Error(unaryExpNode.getIdent().getLineNumber(),ErrorType.d));
                }
                List<Integer> funcFParamDimensions = new ArrayList<>();
                for (FuncParam funcParam : funcSymbol.getFuncParams()) {
                    funcFParamDimensions.add(funcParam.getDimension());
                }

                List<Integer> funcRParamDimensions=new ArrayList<>();
                if(unaryExpNode.getFuncRParamsNode()!=null){
                    funcRParamsError(unaryExpNode.getFuncRParamsNode());
                    for(ExpNode expNode :unaryExpNode.getFuncRParamsNode().getExpNodes()){
                        FuncParam funcRParam=getFuncParamInExp(expNode);
                        if(funcRParam!=null){
                            if(funcRParam.getName()==null){
                                funcRParamDimensions.add(funcRParam.getDimension());
                            }else{
                                Symbol symbol2=get(funcRParam.getName());
                                if(symbol2 instanceof ArraySymbol){
                                    funcRParamDimensions.add(((ArraySymbol) symbol2).getDimension()-funcRParam.getDimension());
                                }else if(symbol2 instanceof FuncSymbol){
                                    funcRParamDimensions.add(((FuncSymbol) symbol2).getType()==FuncType.VOID?-1:0);
                                }
                            }
                        }
                    }
                }
                if(!Objects.equals(funcFParamDimensions,funcRParamDimensions)){
                    ErrorHandler.getInstance().addError(new Error(unaryExpNode.getIdent().getLineNumber(),ErrorType.e));
                }
            }
        }
    }



    private void funcRParamsError(FuncRParamsNode funcRParamsNode) {
        // FuncRParams -> Exp { ',' Exp }
        for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
            expError(expNode);
        }
    }

    private void mulExpError(MulExpNode mulExpNode) {
        //  MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
//        unaryExpError(mulExpNode.getUnaryExpNode());
//        if (mulExpNode.getMulExpNode() != null) {
//            mulExpError(mulExpNode.getMulExpNode());
//        }
        if(mulExpNode.getOperator()==null){
            unaryExpError(mulExpNode.getUnaryExpNode());
        }else{
            mulExpError(mulExpNode.getMulExpNode());
            unaryExpError(mulExpNode.getUnaryExpNode());
        }
    }



    private void addExpError(AddExpNode addExpNode) {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
//        mulExpError(addExpNode.getMulExpNode());
//        if (addExpNode.getAddExpNode() != null) {
//            addExpError(addExpNode.getAddExpNode());
//        }
        if(addExpNode.getOperator()==null){
            mulExpError(addExpNode.getMulExpNode());
        }else{
            addExpError(addExpNode.getAddExpNode());
            mulExpError(addExpNode.getMulExpNode());
        }
    }



    private void relExpError(RelExpNode relExpNode) {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        if(relExpNode.getOperator()==null){
            addExpError(relExpNode.getAddExpNode());
        }else{
            relExpError(relExpNode.getRelExpNode());
            addExpError(relExpNode.getAddExpNode());
        }
    }

    private void eqExpError(EqExpNode eqExpNode) {
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
        if(eqExpNode.getOperator()==null){
            relExpError(eqExpNode.getRelExpNode());
        }else{
            eqExpError(eqExpNode.getEqExpNode());
            relExpError(eqExpNode.getRelExpNode());
        }
    }

    private void lAndExpError(LAndExpNode lAndExpNode) {
        //  LAndExp → EqExp | LAndExp '&&' EqExp
        if(lAndExpNode.getAndToken()==null){
            eqExpError(lAndExpNode.getEqExpNode());
        }else{
            lAndExpError(lAndExpNode.getLAndExpNode());
            eqExpError(lAndExpNode.getEqExpNode());
        }
    }

    private void lOrExpError(LOrExpNode lOrExpNode) {
        //  LOrExp → LAndExp | LOrExp '||' LAndExp
        if(lOrExpNode.getOrToken()==null) {
            lAndExpError(lOrExpNode.getLAndExpNode());
        }else{
            lOrExpError(lOrExpNode.getLOrExpNode());
            lAndExpError(lOrExpNode.getLAndExpNode());
        }
    }

    private void constExpError(ConstExpNode constExpNode) {
        // ConstExp -> AddExp
        addExpError(constExpNode.getAddExpNode());
    }

}
