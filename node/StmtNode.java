package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class StmtNode {
    // Stmt -> LVal '=' Exp ';'
    //	| [Exp] ';'
    //	| Block
    //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //	| 'while' '(' Cond ')' Stmt
    //	| 'break' ';' | 'continue' ';'
    //	| 'return' [Exp] ';'
 //'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
    public enum StmtType {
        LValAssignExp, Exp, Block, If, For, Break, Continue, Return, Printf
    }

    private StmtType type;
    private LValNode lValNode;
    private Token assignToken;
    private ExpNode expNode;
    private List<Token> semicnTokens;
    private BlockNode blockNode;
    private Token ifToken;
    private Token leftParentToken;
    private CondNode condNode;
    private Token rightParentToken;
    private List<StmtNode> stmtNodes;
    private Token elseToken;

    private Token forToken;
    //private List<ForStmtNode> forStmtNode;
    private ForStmtNode forStmtNode1;
    private ForStmtNode forStmtNode2;

    private Token breakOrContinueToken;
    private Token returnToken;
    //private Token getintToken;
    private Token printfToken;
    private Token formatString;
    private List<Token> commas;
    private List<ExpNode> expNodes;

    public StmtNode(StmtType type, LValNode lValNode, Token assignToken, ExpNode expNode, List<Token> semicnToken) {
        // LVal '=' Exp ';'
        this.type = type;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.expNode = expNode;
        this.semicnTokens = semicnToken;
    }

    public StmtNode(StmtType type, ExpNode expNode, List<Token> semicnToken) {
        // [Exp] ';'
        this.type = type;
        this.expNode = expNode;
        this.semicnTokens = semicnToken;
    }

    public StmtNode(StmtType type, BlockNode blockNode) {
        // Block
        this.type = type;
        this.blockNode = blockNode;
    }

    public StmtNode(StmtType type, Token ifToken, Token leftParentToken, CondNode condNode, Token rightParentToken, List<StmtNode> stmtNodes, Token elseToken) {
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        this.type = type;
        this.ifToken = ifToken;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
        this.elseToken = elseToken;
    }

    public StmtNode(StmtType type,Token forToken ,ForStmtNode forStmtNode1,ForStmtNode forStmtNode2, List<Token> semicnTokens,Token leftParentToken, CondNode condNode, Token rightParentToken, List<StmtNode> stmtNodes) {
        //'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        this.type = type;
        this.forToken= forToken;
        this.forStmtNode1 = forStmtNode1;
        this.forStmtNode2 = forStmtNode2;
        this.semicnTokens=semicnTokens;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
    }

    public StmtNode(StmtType type, Token breakOrContinueToken, List<Token> semicnToken) {
        // 'break' ';'
        this.type = type;
        this.breakOrContinueToken = breakOrContinueToken;
        this.semicnTokens = semicnToken;
    }

    public StmtNode(StmtType type, Token returnToken, ExpNode expNode, List<Token> semicnToken) {
        // 'return' [Exp] ';'
        this.type = type;
        this.returnToken = returnToken;
        this.expNode = expNode;
        this.semicnTokens = semicnToken;
    }



    public StmtNode(StmtType type, Token printfToken, Token leftParentToken, Token formatString, List<Token> commas, List<ExpNode> expNodes, Token rightParentToken, List<Token> semicnToken) {
        // 'printf' '(' FormatString { ',' Exp } ')' ';'
        this.type = type;
        this.printfToken = printfToken;
        this.leftParentToken = leftParentToken;
        this.formatString = formatString;
        this.commas = commas;
        this.expNodes = expNodes;
        this.rightParentToken = rightParentToken;
        this.semicnTokens = semicnToken;
    }

    public StmtType getType() {
        return type;
    }

    public LValNode getLValNode() {
        return lValNode;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public List<Token> getSemicnToken() {
        return semicnTokens;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getIfToken() {
        return ifToken;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public CondNode getCondNode() {
        return condNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public List<StmtNode> getStmtNodes() {
        return stmtNodes;
    }

    public Token getElseToken() {
        return elseToken;
    }

    public ForStmtNode getForStmtNode1() {
        return forStmtNode1;
    }

    public ForStmtNode getForStmtNode2() {
        return forStmtNode2;
    }

    public Token getBreakOrContinueToken() {
        return breakOrContinueToken;
    }

//    public Token getGetintToken() {
//        return getintToken;
//    }

    public Token getPrintfToken() {
        return printfToken;
    }

    public Token getFormatString() {
        return formatString;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public Token getReturnToken() {
        return returnToken;
    }

    public void print() throws IOException {
        switch (type) {
            case LValAssignExp:
                // LVal '=' Exp ';'
                lValNode.print();
                IOUtils.write(assignToken.toString());
                expNode.print();
                IOUtils.write(semicnTokens.get(0).toString());
                break;
            case Exp:
                // [Exp] ';'
                if (expNode != null) expNode.print();
                IOUtils.write(semicnTokens.get(0).toString());
                break;
            case Block:
                // Block
                blockNode.print();
                break;
            case If:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                IOUtils.write(ifToken.toString());
                IOUtils.write(leftParentToken.toString());
                condNode.print();
                IOUtils.write(rightParentToken.toString());
                stmtNodes.get(0).print();
                if (elseToken != null) {
                    IOUtils.write(elseToken.toString());
                    stmtNodes.get(1).print();
                }
                break;
            case For:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                IOUtils.write(forToken.toString());
                IOUtils.write(leftParentToken.toString());
                if(forStmtNode1!=null){
                    forStmtNode1.print();
                }
                IOUtils.write(semicnTokens.get(0).toString());

                if(condNode!=null){
                    condNode.print();
                }
                IOUtils.write(semicnTokens.get(1).toString());

                if(forStmtNode2!=null){
                    forStmtNode2.print();
                }
                IOUtils.write(rightParentToken.toString());
                stmtNodes.get(0).print();
                break;
            case Break:

                // 'break' ';'
            case Continue:
                // 'continue' ';'
                IOUtils.write(breakOrContinueToken.toString());
                IOUtils.write(semicnTokens.get(0).toString());
                break;
            case Return:
                // 'return' [Exp] ';'
                IOUtils.write(returnToken.toString());
                if (expNode != null) {
                    expNode.print();
                }
                IOUtils.write(semicnTokens.get(0).toString());
                break;
            case Printf:
                // 'printf' '(' FormatString { ',' Exp } ')' ';'
                IOUtils.write(printfToken.toString());
                IOUtils.write(leftParentToken.toString());
                IOUtils.write(formatString.toString());
                for (int i = 0; i < expNodes.size(); i++) {
                    IOUtils.write(commas.get(i).toString());
                    expNodes.get(i).print();
                }
                IOUtils.write(rightParentToken.toString());
                IOUtils.write(semicnTokens.get(0).toString());
                break;
        }
        IOUtils.write(Parser.nodeType.get(NodeType.Stmt));
    }
}
