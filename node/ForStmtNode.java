package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class ForStmtNode {
    //ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    private List<LValNode> lValNode;
    private List<Token>  equal;
    private List<ExpNode> expNode;
    private List<Token> commas;

    public ForStmtNode(List<LValNode> lValNode, List<Token> equal, List<ExpNode> expNode, List<Token> commas) {
        this.lValNode = lValNode;
        this.equal = equal;
        this.expNode = expNode;
        this.commas = commas;
    }

    public List<LValNode> getlValNode() {
        return lValNode;
    }

    public List<Token> getEqual() {
        return equal;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<ExpNode> getExpNode() {
        return expNode;
    }



    public void print() throws IOException {
        lValNode.get(0).print();
        IOUtils.write(equal.get(0).toString());
        expNode.get(0).print();


        for(int i=1;i<lValNode.size();i++){
            IOUtils.write(commas.get(i-1).toString());
            lValNode.get(i).print();
            IOUtils.write(equal.get(i).toString());
            expNode.get(i).print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.ForStmt));
    }
}
