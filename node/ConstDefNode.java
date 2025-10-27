package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class ConstDefNode {
    //ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private Token ident;
    private List<ConstExpNode> constExpNodes;
    private List<Token> leftBrackets;
    private List<Token> rightBrackets;
    private Token equalToken;
    private ConstInitValNode constInitValNode;

    public ConstDefNode(Token ident, List<ConstExpNode> constExpNodes, List<Token> leftBrackets, Token equalToken, List<Token> rightBrackets, ConstInitValNode constInitValNode) {
        this.ident = ident;
        this.constExpNodes = constExpNodes;
        this.leftBrackets = leftBrackets;
        this.equalToken = equalToken;
        this.rightBrackets = rightBrackets;
        this.constInitValNode = constInitValNode;
    }

    public Token getIdent() {
        return ident;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public List<Token> getLeftBrackets() {
        return leftBrackets;
    }

    public List<Token> getRightBrackets() {
        return rightBrackets;
    }

    public Token getEqualToken() {
        return equalToken;
    }

    public ConstInitValNode getConstInitValNode() {
        return constInitValNode;
    }

    public void print() throws IOException {
        IOUtils.write(ident.toString());
        for (int i = 0; i < constExpNodes.size(); i++) {
            IOUtils.write(leftBrackets.get(i).toString());
            constExpNodes.get(i).print();
            IOUtils.write(rightBrackets.get(i).toString());
        }
        IOUtils.write(equalToken.toString());
        constInitValNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDef));
    }

}
