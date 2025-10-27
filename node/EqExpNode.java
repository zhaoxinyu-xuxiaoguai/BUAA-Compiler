package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class EqExpNode {
    // RelExp | EqExp ('==' | '!=') RelExp
    private RelExpNode relExpNode;
    private Token operator;
    private EqExpNode eqExpNode;

    public EqExpNode(RelExpNode relExpNode, Token operator, EqExpNode eqExpNode) {
        this.relExpNode = relExpNode;
        this.operator = operator;
        this.eqExpNode = eqExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public void print() throws IOException {
//        relExpNode.print();
//        if (operator != null) {
//            IOUtils.write(operator.toString());
//            eqExpNode.print();
//        }
//        IOUtils.write(Parser.nodeType.get(NodeType.EqExp));
        if(operator==null){
            relExpNode.print();
        }else{
            eqExpNode.print();
            IOUtils.write(operator.toString());
            relExpNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.EqExp));
    }
}
