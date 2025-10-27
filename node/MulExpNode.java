package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class MulExpNode {
    // MulExp -> UnaryExp / MulExp ('*' / '/' /'%') UnaryExp
    private UnaryExpNode unaryExpNode;
    private Token operator;
    private MulExpNode mulExpNode;

    public MulExpNode(UnaryExpNode unaryExpNode, Token operator, MulExpNode mulExpNode) {
        this.unaryExpNode = unaryExpNode;
        this.operator = operator;
        this.mulExpNode = mulExpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public void print() throws IOException {
//        unaryExpNode.print();
//        IOUtils.write(Parser.nodeType.get(NodeType.MulExp));
//        if(operator!=null){
//            IOUtils.write(operator.toString());
//            mulExpNode.print();
//        }
//        unaryExpNode.print();
//        if(operator!=null){
//            mulExpNode.print();
//            IOUtils.write(operator.toString());
//
//        }
//       IOUtils.write(Parser.nodeType.get(NodeType.MulExp));
        if(operator==null){
            unaryExpNode.print();
        }else{
            mulExpNode.print();
            IOUtils.write(operator.toString());
            unaryExpNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.MulExp));
    }
    public String getStr(){
        return unaryExpNode.getStr()+(operator==null ? "" :operator.getContent() + mulExpNode.getStr());
    }
}
