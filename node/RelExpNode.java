package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class RelExpNode {
    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private AddExpNode addExpNode;
    private Token operator;
    private RelExpNode relExpNode;


    public RelExpNode(AddExpNode addExpNode, Token operator, RelExpNode relExpNode) {
        this.addExpNode = addExpNode;
        this.operator = operator;
        this.relExpNode = relExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public void print() throws IOException {
//        addExpNode.print();
//        IOUtils.write(Parser.nodeType.get(NodeType.RelExp));
//        if (operator != null) {
//            IOUtils.write(operator.toString());
//            relExpNode.print();
//        }
//        // 1. 先递归处理子节点（深度优先遍历）
        if (operator == null) {
            // 情况1：RelExp -> AddExp
            addExpNode.print(); // 先打印 AddExp 的子节点
        } else {
            // 情况2：RelExp -> RelExp ('<'|'>'|'<='|'>=') AddExp
            relExpNode.print();       // 先递归处理左子树
            IOUtils.write(operator.toString()); // 打印运算符（如 '<' 或 '>=')
            addExpNode.print();       // 再递归处理右子树
        }
        // 2. 最后打印当前非终结符 "<RelExp>"
        IOUtils.write(Parser.nodeType.get(NodeType.RelExp));
    }
}
