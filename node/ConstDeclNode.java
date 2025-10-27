package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class ConstDeclNode {
    // ConstDecl -> 'const' Btype ConstDef { ',' ConstDef } ';'
    private Token constToken;
    private BTypeNode bTypeNode;
    private List<ConstDefNode> constDefNodeList;
    private List<Token> commas;
    private Token semicnToken;

    public ConstDeclNode(Token constToken, BTypeNode bTypeNode, List<ConstDefNode> constDefNodeList, List<Token> commas, Token semicnToken) {
        this.constToken = constToken;
        this.bTypeNode = bTypeNode;
        this.constDefNodeList = constDefNodeList;
        this.commas = commas;
        this.semicnToken = semicnToken;
    }

    public Token getConstToken() {
        return constToken;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public List<ConstDefNode> getConstDefNodeList() {
        return constDefNodeList;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getSemicnToken() {
        return semicnToken;
    }

    public void print() throws IOException {
        IOUtils.write(constToken.toString());
        bTypeNode.print();
        constDefNodeList.get(0).print();
        for(int i=1;i<constDefNodeList.size();i++){
            IOUtils.write(commas.get(i-1).toString());
            constDefNodeList.get(i).print();
        }
        IOUtils.write(semicnToken.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDecl));
    }
}
