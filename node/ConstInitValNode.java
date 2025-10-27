package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class ConstInitValNode {
    // ConstInitVal -> ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    private List<ConstExpNode> constExpNode;
    private Token leftBraceToken;
    //private List<ConstInitValNode> constInitValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public ConstInitValNode(List<ConstExpNode> constExpNode, Token leftBraceToken, List<Token> commas, Token rightBraceToken) {
        this.constExpNode = constExpNode;
        this.leftBraceToken = leftBraceToken;
        //this.constInitValNodes = constInitValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public List<ConstExpNode> getConstExpNode() {
        return constExpNode;
    }

    public Token getLeftBraceToken() {
        return leftBraceToken;
    }

//    public List<ConstInitValNode> getConstInitValNodes() {
//        return constInitValNodes;
//    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getRightBraceToken() {
        return rightBraceToken;
    }

    public void print() throws IOException {
        if (constExpNode.size()==1) {
            constExpNode.get(0).print();
        } else {
            IOUtils.write(leftBraceToken.toString());
            if(!constExpNode.isEmpty()){
                constExpNode.get(0).print();
            }

            for (int i = 1; i < constExpNode.size(); i++) {
                IOUtils.write(commas.get(i - 1).toString());
                constExpNode.get(i).print();
            }
            IOUtils.write(rightBraceToken.toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.ConstInitVal));
    }
}
