package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class VarDeclNode {
    //  [ 'static' ] BType VarDef { ',' VarDef } ';'
    private Token staticToken;
    private BTypeNode bTypeNode;
    private List<VarDefNode> varDefNodes;
    private List<Token> commas;
    private Token semicn;

    public VarDeclNode(Token staticToken, BTypeNode bTypeNode, List<VarDefNode> varDefNodes, List<Token> commas, Token semicn) {
        this.staticToken = staticToken;
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
        this.commas = commas;
        this.semicn = semicn;
    }

    public List<VarDefNode> getVarDefNodes() {
        return varDefNodes;
    }

    public Token getStaticToken() {
        return staticToken;
    }

    public void print() throws IOException {
        if(staticToken!=null){
            IOUtils.write(staticToken.toString());
        }
        bTypeNode.print();
        varDefNodes.get(0).print();
        for (int i = 1; i < varDefNodes.size(); i++) {
            IOUtils.write(commas.get(i - 1).toString());
            varDefNodes.get(i).print();
        }
        IOUtils.write(semicn.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.VarDecl));
    }
}
