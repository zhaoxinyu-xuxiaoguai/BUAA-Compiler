package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class FuncFParamNode {
    // FuncFParam -> BType Ident [ '[' ']' ]

    private BTypeNode bTypeNode;
    private Token ident;
    private Token leftBrackets;
    private Token rightBrackets;
    //private List<ConstExpNode> constExpNodes;


    public FuncFParamNode(BTypeNode bTypeNode, Token ident, Token leftBrackets, Token rightBrackets) {
        this.bTypeNode = bTypeNode;
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.rightBrackets = rightBrackets;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public void setbTypeNode(BTypeNode bTypeNode) {
        this.bTypeNode = bTypeNode;
    }

    public Token getIdent() {
        return ident;
    }

    public void setIdent(Token ident) {
        this.ident = ident;
    }

    public Token getLeftBrackets() {
        return leftBrackets;
    }

    public void setLeftBrackets(Token leftBrackets) {
        this.leftBrackets = leftBrackets;
    }

    public Token getRightBrackets() {
        return rightBrackets;
    }

    public void setRightBrackets(Token rightBrackets) {
        this.rightBrackets = rightBrackets;
    }

    public void print() throws IOException {
        bTypeNode.print();
        IOUtils.write(ident.toString());
        if (leftBrackets!=null) {
            IOUtils.write(leftBrackets.toString());
            IOUtils.write(rightBrackets.toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncFParam));
    }
}
