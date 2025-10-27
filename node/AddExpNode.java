package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class AddExpNode {
    // AddExp -> MulExp | AddExp ('+' / '-') MulExp
    private MulExpNode mulExpNode;
    private Token operator;
    private AddExpNode addExpNode;

    public AddExpNode(AddExpNode addExpNode,MulExpNode mulExpNode, Token operator ) {
        this.mulExpNode = mulExpNode;
        this.operator = operator;
        this.addExpNode = addExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    //print最后写
    public void print() throws IOException {
//        mulExpNode.print();
//        IOUtils.write(Parser.nodeType.get(NodeType.AddExp));
//        if(operator!=null){
//            IOUtils.write(operator.toString());
//            addExpNode.print();
//        }


        // 1. 先递归处理子节点（深度优先遍历）
        if (operator == null ) {
            // 情况1：AddExp -> MulExp
            mulExpNode.print(); // 先打印 MulExp 的子节点
        } else {
            // 情况2：AddExp -> AddExp ('+' | '-') MulExp
            addExpNode.print();       // 先递归处理左子树
            IOUtils.write(operator.toString()); // 打印运算符（如 '+' 或 '-'）
            mulExpNode.print();       // 再递归处理右子树
        }

        // 2. 最后打印当前非终结符 "<AddExp>"
        IOUtils.write(Parser.nodeType.get(NodeType.AddExp));


     }

    public String getStr(){
        return mulExpNode.getStr()+(operator==null ? "" :operator.getContent() + addExpNode.getStr());
    }
}
