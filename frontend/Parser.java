package frontend;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import node.*;
import token.Token;
import token.TokenType;

import java.io.IOException;
import java.util.*;

public class Parser {
    private static final Parser instance =new Parser();

    public static Parser getInstance(){
        return instance;
    }

    private List<Token> tokens;
    private int index=0;
    private Token now;
    private CompUnitNode compUnitNode;

    public void setTokens(List<Token> tokens){
        this.tokens=tokens;
        now = tokens.get(index);
    }

    public CompUnitNode getCompUnitNode() {
        return compUnitNode;
    }
    public void analyze() {
        this.compUnitNode = CompUnit();
    }

    public static Map<NodeType,String> nodeType =new HashMap<NodeType, String>(){{
        put(NodeType.CompUnit, "<CompUnit>\n");
        put(NodeType.Decl, "<Decl>\n");
        put(NodeType.ConstDecl, "<ConstDecl>\n");
        put(NodeType.BType, "<BType>\n");
        put(NodeType.ConstDef, "<ConstDef>\n");
        put(NodeType.ConstInitVal, "<ConstInitVal>\n");
        put(NodeType.VarDecl, "<VarDecl>\n");
        put(NodeType.VarDef, "<VarDef>\n");
        put(NodeType.InitVal, "<InitVal>\n");
        put(NodeType.FuncDef, "<FuncDef>\n");
        put(NodeType.MainFuncDef, "<MainFuncDef>\n");
        put(NodeType.FuncType, "<FuncType>\n");
        put(NodeType.FuncFParams, "<FuncFParams>\n");
        put(NodeType.FuncFParam, "<FuncFParam>\n");
        put(NodeType.Block, "<Block>\n");
        put(NodeType.BlockItem, "<BlockItem>\n");
        put(NodeType.Stmt, "<Stmt>\n");
        put(NodeType.Exp, "<Exp>\n");
        put(NodeType.Cond, "<Cond>\n");
        put(NodeType.LVal, "<LVal>\n");
        put(NodeType.PrimaryExp, "<PrimaryExp>\n");
        put(NodeType.Number, "<Number>\n");
        put(NodeType.UnaryExp, "<UnaryExp>\n");
        put(NodeType.UnaryOp, "<UnaryOp>\n");
        put(NodeType.FuncRParams, "<FuncRParams>\n");
        put(NodeType.MulExp, "<MulExp>\n");
        put(NodeType.AddExp, "<AddExp>\n");
        put(NodeType.RelExp, "<RelExp>\n");
        put(NodeType.EqExp, "<EqExp>\n");
        put(NodeType.LAndExp, "<LAndExp>\n");
        put(NodeType.LOrExp, "<LOrExp>\n");
        put(NodeType.ConstExp, "<ConstExp>\n");
        put(NodeType.ForStmt,"<ForStmt>\n");
    }};

    private  Token match(TokenType tokenType){
        if(now.getType()==tokenType){
            Token tmp=now;
            if(index<tokens.size()-1){
                now=tokens.get(++index);
            }
            return tmp;
        }else if(tokenType == TokenType.SEMICN){
            ErrorHandler.getInstance().addError((new Error(tokens.get(index-1).getLineNumber(), ErrorType.i)));
            // 合成分号时前进一个 token，避免零消费

            return new Token(TokenType.SEMICN,tokens.get(index-1).getLineNumber(),";");
        }else if(tokenType==TokenType.RPARENT){
            ErrorHandler.getInstance().addError(new Error(tokens.get(index-1).getLineNumber(),ErrorType.j));

            return new Token(TokenType.RPARENT,tokens.get(index-1).getLineNumber(),")" );
        }else if(tokenType==TokenType.RBRACK){
            ErrorHandler.getInstance().addError(new Error(tokens.get(index-1).getLineNumber(),ErrorType.k));

            return new Token(TokenType.RBRACK,tokens.get(index-1).getLineNumber(),"]" );
        }else{
            throw new RuntimeException("Syntax error at line " + now.getLineNumber() + ": " + now.getContent() + " is not " + tokenType);
        }
    }
    private CompUnitNode CompUnit(){
        // CompUnit -> {Decl}{FuncDef} MainFuncDef
        List<DeclNode> declNodes = new ArrayList<>();
        List<FuncDefNode> funcDefNodes=new ArrayList<>();
        MainFuncDefNode mainFuncDefNode=null;
        while (tokens.get(index+2).getType()!=TokenType.LPARENT){
            DeclNode declNode = Decl();
            declNodes.add(declNode);
        }
        while(tokens.get(index+1).getType()!=TokenType.MAINTK){
            FuncDefNode funcDefNode=FuncDef();
            funcDefNodes.add(funcDefNode);
        }
        mainFuncDefNode=mainFuncDef();
        return new CompUnitNode(declNodes, funcDefNodes, mainFuncDefNode);
    }
    private ConstExpNode ConstExp(){
        //ConstExp->AddExp
        return new ConstExpNode(AddExp());
    }
    private DeclNode Decl(){
        //Decl -> ConstDecl / VarDecl
        ConstDeclNode constDeclNode =null;
        VarDeclNode varDeclNode=null;
        //begin
        if(now.getType()==TokenType.CONSTTK){
            constDeclNode=ConstDecl();
        }else{
            varDeclNode=VarDecl();
        }
        //end
        return new DeclNode(constDeclNode,varDeclNode);
    }

    private ConstDeclNode ConstDecl(){
        //ConstDecl -> 'const' BType ConstDef { ',' ConstDef} ';'
        Token constToken = match(TokenType.CONSTTK);
        BTypeNode bTypeNode= BType();
        List<ConstDefNode> constDefNodes=new ArrayList<>();
        List<Token> commas=new ArrayList<>();
        Token semicnToken=null;

        //begin
        constDefNodes.add(ConstDef());
        while(now.getType()==TokenType.COMMA){
            commas.add(match(TokenType.COMMA));
            constDefNodes.add(ConstDef());
        }
        semicnToken=match(TokenType.SEMICN);
        //end


        return new ConstDeclNode(constToken,bTypeNode,constDefNodes,commas,semicnToken );
    }

    private BTypeNode BType(){
        //BType -> 'int'
        Token bTypeToken=match(TokenType.INTTK);
        return new BTypeNode(bTypeToken);
    }

    private ConstDefNode ConstDef(){
        //ConstDef -> Ident [ '[' ConstExp ']' ] '=' ConstInitVal
        Token ident = match(TokenType.IDENFR);
        List<Token> leftBrackets=new ArrayList<>();
        List<ConstExpNode> constExpNodes=new ArrayList<>();
        List<Token> rightBrackets=new ArrayList<>();
        Token equalToken;
        while(now.getType()==TokenType.LBRACK){
            leftBrackets.add(match(TokenType.LBRACK));
            constExpNodes.add(ConstExp());
            rightBrackets.add(match(TokenType.RBRACK));
        }
        equalToken=match(TokenType.ASSIGN);
        ConstInitValNode constInitValNode=ConstInitVal();

        return new ConstDefNode(ident,constExpNodes,leftBrackets,equalToken,rightBrackets,constInitValNode);
    }

    private ConstInitValNode ConstInitVal(){
        //ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
        List<ConstExpNode> constExpNode=new ArrayList<>();
        Token leftBraceToken=null;
        List<Token> commas=new ArrayList<>();
        Token rightBraceToken=null;

        if(now.getType()== TokenType.LBRACE){
            leftBraceToken=match(TokenType.LBRACE);
            if(now.getType()!= TokenType.RBRACE){
                constExpNode.add(ConstExp());
                while(now.getType()!= TokenType.RBRACE){
                    commas.add(match(TokenType.COMMA));
                    constExpNode.add(ConstExp());
                }
            }
            rightBraceToken=match(TokenType.RBRACE);
        }else{
            constExpNode.add(ConstExp());
        }

        return new ConstInitValNode(constExpNode,leftBraceToken,commas,rightBraceToken);
    }

    private VarDeclNode VarDecl(){
        //VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
        Token staticToken=null;
        if(now.getType()==TokenType.STATICTK){
            staticToken=match(TokenType.STATICTK);
        }

        BTypeNode bTypeNode=BType();
        List<VarDefNode> varDefNodes=new ArrayList<>();
        List<Token> commas=new ArrayList<>();
        Token semicnToken;
        varDefNodes.add(VarDef());

        while(now.getType()==TokenType.COMMA){
            commas.add(match(TokenType.COMMA));
            varDefNodes.add(VarDef());
        }
        semicnToken=match(TokenType.SEMICN);
        return new VarDeclNode(staticToken,bTypeNode, varDefNodes, commas, semicnToken);
    }

    private VarDefNode VarDef(){
        // VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
        Token ident=match(TokenType.IDENFR);
        Token leftBrack=null;
        ConstExpNode constExpNode=null;
        Token rightBrack=null;
        Token assignToken=null;
        InitValNode initValNode=null;
        if(now.getType()==TokenType.LBRACK){
            leftBrack=match(TokenType.LBRACK);
            constExpNode=ConstExp();
            rightBrack=match(TokenType.RBRACK);
        }
        if(now.getType()==TokenType.ASSIGN){
            assignToken=match(TokenType.ASSIGN);
            initValNode=InitVal();
        }

        return new VarDefNode(ident, leftBrack, constExpNode,  rightBrack, assignToken,  initValNode);
    }

    private InitValNode InitVal(){
        //InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
        List<ExpNode> expNodes=new ArrayList<>();
        Token leftBrace = null;
        List<Token> commas=new ArrayList<>();
        Token rightBrace=null;
        if(now.getType()==TokenType.LBRACE){
            leftBrace=match(TokenType.LBRACE);
            if(now.getType()!= TokenType.RBRACE){
                expNodes.add(Exp());
                while(now.getType()== TokenType.COMMA){
                    commas.add(match(TokenType.COMMA));
                    expNodes.add(Exp());
                }
            }
            rightBrace=match(TokenType.RBRACE);
        }else{
            expNodes.add(Exp());
        }

        return new InitValNode(expNodes,leftBrace,commas,rightBrace);
    }

    private FuncDefNode FuncDef(){
        //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block

        FuncTypeNode funcTypeNode = FuncType();
        Token ident = match(TokenType.IDENFR);
        Token leftParentToken = match(TokenType.LPARENT);
        FuncFParamsNode funcParamsNode = null;
        if (now.getType() == TokenType.INTTK) {
            funcParamsNode = FuncFParams();
        }
        Token rightParentToken = match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new FuncDefNode(funcTypeNode, ident, leftParentToken, funcParamsNode, rightParentToken, blockNode);
    }

    private MainFuncDefNode mainFuncDef(){
        //MainFuncDef → 'int' 'main' '(' ')' Block
        Token intToken = match(TokenType.INTTK);
        Token mainToken = match(TokenType.MAINTK);
        Token leftParentToken = match(TokenType.LPARENT);
        Token rightParentToken = match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new MainFuncDefNode(intToken, mainToken, leftParentToken, rightParentToken, blockNode);
    }

    private FuncTypeNode FuncType(){
        // FuncType → 'void' | 'int'
        if (now.getType() == TokenType.VOIDTK) {
            Token voidToken = match(TokenType.VOIDTK);
            return new FuncTypeNode(voidToken);
        } else {
            Token intToken = match(TokenType.INTTK);
            return new FuncTypeNode(intToken);
        }
    }

    private FuncFParamsNode FuncFParams(){
        // FuncFParams → FuncFParam { ',' FuncFParam }
        List<FuncFParamNode> funcFParamNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        funcFParamNodes.add(FuncFParam());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            funcFParamNodes.add(FuncFParam());
        }
        return new FuncFParamsNode(funcFParamNodes, commas);
    }

    private FuncFParamNode FuncFParam(){
        //BType Ident ['[' ']']
        BTypeNode bTypeNode=BType();
        Token identToken=match(TokenType.IDENFR);
        Token leftBrack=null;
        Token rightBrack=null;
        if(now.getType()==TokenType.LBRACK){
            leftBrack=match(TokenType.LBRACK);
            rightBrack=match(TokenType.RBRACK);
        }
        return new FuncFParamNode(bTypeNode, identToken, leftBrack, rightBrack);
    }

    private BlockNode Block(){
        // '{' { BlockItem } '}'
        Token leftBraceToken = match(TokenType.LBRACE);
        List<BlockItemNode> blockItemNodes = new ArrayList<>();
        while (now.getType() != TokenType.RBRACE) {
            blockItemNodes.add(BlockItem());
        }
        Token rightBraceToken = match(TokenType.RBRACE);
        return new BlockNode(leftBraceToken, blockItemNodes, rightBraceToken);
    }

    private BlockItemNode BlockItem(){
        //Decl | Stmt
        DeclNode declNode = null;
        StmtNode stmtNode = null;
        if (now.getType() == TokenType.CONSTTK || now.getType() == TokenType.INTTK || now.getType()==TokenType.STATICTK) {
            declNode = Decl();
        } else {
            stmtNode = Stmt();
        }
        return new BlockItemNode(declNode, stmtNode);
    }

    private StmtNode Stmt(){
        /*Stmt → LVal '=' Exp ';'
          | [Exp] ';'
          | Block
          | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
          | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
          | 'break' ';'
          | 'continue' ';'
          | 'return' [Exp] ';'
          | 'printf''('StringConst {','Exp}')'';'
        */
        if (now.getType() == TokenType.LBRACE) {
            // Block
            BlockNode blockNode = Block();
            return new StmtNode(StmtNode.StmtType.Block, blockNode);
        } else if (now.getType() == TokenType.PRINTFTK) {
            // 'printf' '(' FormatString { ',' Exp } ')' ';'
            Token printfToken = match(TokenType.PRINTFTK);
            Token leftParentToken = match(TokenType.LPARENT);
            Token formatString = match(TokenType.STRCON);
            List<Token> commas = new ArrayList<>();
            List<ExpNode> expNodes = new ArrayList<>();
            while (now.getType() == TokenType.COMMA) {
                commas.add(match(TokenType.COMMA));
                expNodes.add(Exp());
            }
            Token rightParentToken = match(TokenType.RPARENT);
            List<Token> semicnToken = new ArrayList<>();
            semicnToken.add(match(TokenType.SEMICN));
            return new StmtNode(StmtNode.StmtType.Printf, printfToken, leftParentToken, formatString, commas, expNodes, rightParentToken, semicnToken);
        } else if (now.getType() == TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            Token ifToken = match(TokenType.IFTK);
            Token leftParentToken = match(TokenType.LPARENT);
            CondNode condNode = Cond();
            Token rightParentToken = match(TokenType.RPARENT);
            List<StmtNode> stmtNodes = new ArrayList<>();
            stmtNodes.add(Stmt());
            Token elseToken = null;
            if (now.getType() == TokenType.ELSETK) {
                elseToken = match(TokenType.ELSETK);
                stmtNodes.add(Stmt());
            }
            return new StmtNode(StmtNode.StmtType.If, ifToken, leftParentToken, condNode, rightParentToken, stmtNodes, elseToken);
        } else if (now.getType() == TokenType.FORTK) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            Token forToken=match(TokenType.FORTK);
            Token leftParentToken = match(TokenType.LPARENT);


            List<ForStmtNode> forStmtNodes=new ArrayList<>();
            ForStmtNode forStmtNode1=null;
            ForStmtNode forStmtNode2=null;
            List<Token> semicons = new ArrayList<>();
            CondNode condNode = null;
            Token rightParentToken = null;
            List<StmtNode> stmtNodes = new ArrayList<>();

            if(now.getType()!=TokenType.SEMICN){
                forStmtNode1=ForStmt();
            }
            semicons.add(match(TokenType.SEMICN));

            if(now.getType()!=TokenType.SEMICN) {
                condNode = Cond();
            }
            semicons.add(match(TokenType.SEMICN));

            if(now.getType()!=TokenType.RPARENT){
                forStmtNode2=ForStmt();
            }
            rightParentToken=match(TokenType.RPARENT);
            stmtNodes.add(Stmt());
            return new StmtNode(StmtNode.StmtType.For, forToken ,forStmtNode1,forStmtNode2,semicons,leftParentToken,condNode,rightParentToken,stmtNodes);


        } else if (now.getType() == TokenType.BREAKTK) {
            // 'break' ';'
            Token breakToken = match(TokenType.BREAKTK);
            List<Token> semicnToken=new ArrayList<>();
            semicnToken.add(match(TokenType.SEMICN));
            return new StmtNode(StmtNode.StmtType.Break, breakToken, semicnToken);
        } else if (now.getType() == TokenType.CONTINUETK) {
            // 'continue' ';'
            Token continueToken = match(TokenType.CONTINUETK);
            List<Token> semicnToken=new ArrayList<>();
            semicnToken.add(match(TokenType.SEMICN));
            return new StmtNode(StmtNode.StmtType.Continue, continueToken, semicnToken);
        } else if (now.getType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            Token returnToken = match(TokenType.RETURNTK);
            ExpNode expNode = null;
            if (now.getType()!=TokenType.SEMICN) {
                expNode = Exp();
            }
            List<Token> semicnToken=new ArrayList<>();
            semicnToken.add(match(TokenType.SEMICN));
            return new StmtNode(StmtNode.StmtType.Return, returnToken, expNode, semicnToken);
        } else {
            boolean containsAssign=false;
            for (int i = index; i < tokens.size() ; i++) {
                if (tokens.get(i).getType() == TokenType.ASSIGN) {
                    containsAssign=true;
                }if(tokens.get(i).getType()==TokenType.SEMICN){
                    break;
                }
            }
            if (containsAssign) {
                // LVal '=' Exp ';'
                LValNode lValNode = LVal();
                Token assignToken = match(TokenType.ASSIGN);
                ExpNode expNode = Exp();
                List<Token> semicnToken = new ArrayList<>();
                semicnToken.add(match(TokenType.SEMICN));
                return new StmtNode(StmtNode.StmtType.LValAssignExp, lValNode, assignToken, expNode, semicnToken);
                
            } else {
                // [Exp] ';'
                ExpNode expNode = null;
                if (now.getType()!=TokenType.SEMICN) {
                    expNode = Exp();
                }
                List<Token> semicnToken = new ArrayList<>();
                semicnToken.add(match(TokenType.SEMICN));
                return new StmtNode(StmtNode.StmtType.Exp, expNode, semicnToken);
            }
        }
    }

    private ForStmtNode ForStmt(){
        // ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
        List<LValNode> lValNodes=new ArrayList<>();
        List<Token> equals=new ArrayList<>();
        List<ExpNode> expNodes=new ArrayList<>();
        List<Token> commas=new ArrayList<>();

        lValNodes.add(LVal());
        equals.add(match(TokenType.ASSIGN));
        expNodes.add(Exp());

        while(now.getType()== TokenType.COMMA){
            commas.add(match(TokenType.COMMA));
            lValNodes.add(LVal());
            equals.add(match(TokenType.ASSIGN));
            expNodes.add(Exp());
        }

        return new ForStmtNode(lValNodes,equals,expNodes,commas);
    }

    private boolean isExp(){
        return now.getType()==TokenType.IDENFR||
               now.getType()==TokenType.PLUS||
               now.getType()==TokenType.MINU||
               now.getType()==TokenType.NOT||
               now.getType()==TokenType.LPARENT||
               now.getType()==TokenType.INTCON;
    }

    private MulExpNode MulExp(){
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
//        UnaryExpNode unaryExpNode = UnaryExp();
//        Token operator = null;
//        MulExpNode mulExpNode = null;
//        if (now.getType() == TokenType.MULT) {
//            operator = match(TokenType.MULT);
//            mulExpNode = MulExp();
//        } else if (now.getType() == TokenType.DIV) {
//            operator = match(TokenType.DIV);
//            mulExpNode = MulExp();
//        } else if (now.getType() == TokenType.MOD) {
//            operator = match(TokenType.MOD);
//            mulExpNode = MulExp();
//        }
        MulExpNode left=new MulExpNode(UnaryExp(), null, null);
        while(now.getType()==TokenType.MULT || now.getType()==TokenType.DIV||now.getType()==TokenType.MOD){
                Token ope=match(now.getType());
                UnaryExpNode right=UnaryExp();
                left=new MulExpNode(right, ope, left);
        }
        return left;
    }

    private AddExpNode AddExp(){
        //AddExp → MulExp | AddExp ('+' | '−') MulExp
//        MulExpNode mulExpNode = MulExp();
//        Token operator = null;
//        AddExpNode addExpNode = null;
//        if (now.getType() == TokenType.PLUS) {
//            operator = match(TokenType.PLUS);
//            addExpNode = AddExp();
//        } else if (now.getType() == TokenType.MINU) {
//            operator = match(TokenType.MINU);
//            addExpNode = AddExp();
//        }
//        return new AddExpNode(mulExpNode, operator, addExpNode);
        AddExpNode left=new AddExpNode(null,MulExp(),null);
        while(now.getType()==TokenType.PLUS||now.getType()==TokenType.MINU){
            Token ope=match(now.getType());
            MulExpNode right=MulExp();
            left=new AddExpNode(left,right,ope);
        }
        return left;
    }

    private  ExpNode Exp(){
        // Exp -> AddExp
        return new ExpNode(AddExp());
    }

    private RelExpNode RelExp(){
        //RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
//        AddExpNode addExpNode = AddExp();
//        Token operator = null;
//        RelExpNode relExpNode = null;
//        if (now.getType() == TokenType.LSS) {
//            operator = match(TokenType.LSS);
//            relExpNode = RelExp();
//        } else if (now.getType() == TokenType.GRE) {
//            operator = match(TokenType.GRE);
//            relExpNode = RelExp();
//        } else if (now.getType() == TokenType.LEQ) {
//            operator = match(TokenType.LEQ);
//            relExpNode = RelExp();
//        } else if (now.getType() == TokenType.GEQ) {
//            operator = match(TokenType.GEQ);
//            relExpNode = RelExp();
//        }
//        return new RelExpNode(addExpNode, operator, relExpNode);
        RelExpNode left=new RelExpNode(AddExp(),null,null);
        while(now.getType()== TokenType.LSS||now.getType()==TokenType.GRE||now.getType()==TokenType.LEQ||now.getType()==TokenType.GEQ){
            Token ope=match(now.getType());
            AddExpNode right=AddExp();
            left=new RelExpNode(right,ope,left);
        }
        return left;
    }

    private EqExpNode EqExp(){
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
//        RelExpNode relExpNode = RelExp();
//        Token operator = null;
//        EqExpNode eqExpNode = null;
//        if (now.getType() == TokenType.EQL) {
//            operator = match(TokenType.EQL);
//            eqExpNode = EqExp();
//        } else if (now.getType() == TokenType.NEQ) {
//            operator = match(TokenType.NEQ);
//            eqExpNode = EqExp();
//        }
//        return new EqExpNode(relExpNode, operator, eqExpNode);
        EqExpNode left=new EqExpNode(RelExp(),null,null);
        while(now.getType()== TokenType.EQL||now.getType()==TokenType.NEQ){
            Token ope=match(now.getType());
            RelExpNode right=RelExp();
            left=new EqExpNode(right,ope,left);
        }
        return left;
    }

    private LAndExpNode LAndExp(){
        // LAndExp → EqExp | LAndExp '&&' EqExp
//        EqExpNode eqExpNode = EqExp();
//        Token operator = null;
//        LAndExpNode lAndExpNode = null;
//        if (now.getType() == TokenType.AND) {
//            operator = match(TokenType.AND);
//            lAndExpNode = LAndExp();
//        }
//        return new LAndExpNode(eqExpNode, operator, lAndExpNode);
        LAndExpNode left=new LAndExpNode(EqExp(), null,null);
        while(now.getType()==TokenType.AND){
            Token ope=match(now.getType());
            EqExpNode right=EqExp();
            left=new LAndExpNode(right,ope,left);
        }
        return left;
    }

    private LOrExpNode LOrExp(){
        //LOrExp → LAndExp | LOrExp '||' LAndExp
        LOrExpNode left=new LOrExpNode(LAndExp(),null,null);
        while(now.getType()==TokenType.OR){
            Token ope=match(now.getType());
            LAndExpNode right=LAndExp();
            left=new LOrExpNode(right,ope,left);
        }
        return left;
    }

    private CondNode Cond(){
        //Cond->LOrExp
        return new CondNode(LOrExp());
    }

    private LValNode LVal(){
        //LVal → Ident ['[' Exp ']']
        Token ident = match(TokenType.IDENFR);
        List<Token> leftBrackets = new ArrayList<>();
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> rightBrackets = new ArrayList<>();
        while (now.getType() == TokenType.LBRACK) {
            leftBrackets.add(match(TokenType.LBRACK));
            expNodes.add(Exp());
            rightBrackets.add(match(TokenType.RBRACK));
        }
        return new LValNode(ident, leftBrackets, expNodes, rightBrackets);
    }

    private PrimaryExpNode PrimaryExp(){
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (now.getType() == TokenType.LPARENT) {
            Token leftParentToken = match(TokenType.LPARENT);
            ExpNode expNode = Exp();
            Token rightParentToken = match(TokenType.RPARENT);
            return new PrimaryExpNode(leftParentToken, expNode, rightParentToken);
        } else if (now.getType() == TokenType.INTCON) {

            NumberNode numberNode = Number();
            return new PrimaryExpNode(numberNode);
        } else {
            LValNode lValNode = LVal();
            return new PrimaryExpNode(lValNode);
        }
    }

    private NumberNode Number(){
        // Number -> IntConst
        return new NumberNode(match(TokenType.INTCON));
    }

    private FuncRParamsNode FuncRParams(){
        // FuncRParams -> Exp { ',' Exp }
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        expNodes.add(Exp());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            expNodes.add(Exp());
        }
        return new FuncRParamsNode(expNodes, commas);
    }
    private UnaryOpNode UnaryOp(){
        // UnaryOp -> '+' | '−' | '!'
        Token token;
        if (now.getType() == TokenType.PLUS) {
            token = match(TokenType.PLUS);
        } else if (now.getType() == TokenType.MINU) {
            token = match(TokenType.MINU);
        } else {
            token = match(TokenType.NOT);
        }
        return new UnaryOpNode(token);
    }
    private UnaryExpNode UnaryExp() {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (now.getType() == TokenType.IDENFR && tokens.get(index + 1).getType() == TokenType.LPARENT) {
            Token ident = match(TokenType.IDENFR);
            Token leftParentToken = match(TokenType.LPARENT);
            FuncRParamsNode funcRParamsNode = null;
            if (isExp()) {
                funcRParamsNode = FuncRParams();
            }
            Token rightParentToken = match(TokenType.RPARENT);
            return new UnaryExpNode(ident, leftParentToken, funcRParamsNode, rightParentToken);
        } else if (now.getType() == TokenType.PLUS || now.getType() == TokenType.MINU || now.getType() == TokenType.NOT) {
            UnaryOpNode unaryOpNode = UnaryOp();
            UnaryExpNode unaryExpNode = UnaryExp();
            return new UnaryExpNode(unaryOpNode, unaryExpNode);
        } else {
            PrimaryExpNode primaryExpNode = PrimaryExp();
            return new UnaryExpNode(primaryExpNode);
        }
    }

    public void printParseAns() throws IOException {

        compUnitNode.print();
    }
}
