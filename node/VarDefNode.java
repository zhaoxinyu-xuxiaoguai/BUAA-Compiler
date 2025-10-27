package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class VarDefNode {
    // VarDef -> Ident  ['[' ConstExp ']']  | Ident  ['[' ConstExp ']']  '=' InitVal
    private Token ident;
    private Token leftBracket;
    private ConstExpNode constExpNodes;
    private Token rightBracket;
    private Token assign;
    private InitValNode initValNode;

    public VarDefNode(Token ident, Token leftBracket, ConstExpNode constExpNodes, Token rightBracket, Token assign, InitValNode initValNode) {
        this.ident = ident;
        this.leftBracket = leftBracket;
        this.constExpNodes = constExpNodes;
        this.rightBracket = rightBracket;
        this.assign = assign;
        this.initValNode = initValNode;
    }

    public Token getIdent() {
        return ident;
    }

    public ConstExpNode getConstExpNodes() {
        return constExpNodes;
    }

    public InitValNode getInitValNode() {
        return initValNode;
    }

    public void print() throws IOException {
        IOUtils.write(ident.toString());
        if(leftBracket!=null){
            IOUtils.write(leftBracket.toString());
            constExpNodes.print();
        }



        if(rightBracket!=null){
            IOUtils.write(rightBracket.toString());
        }


        if (initValNode != null) {
            IOUtils.write(assign.toString());
            initValNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.VarDef));
    }
}
