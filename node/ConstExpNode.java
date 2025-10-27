package node;

import frontend.Parser;
import org.w3c.dom.Node;
import utils.IOUtils;

import java.io.IOException;

public class ConstExpNode {
    //ConstExp -> AddExp
    private AddExpNode addExpNode;

    public ConstExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }
    public void print() throws IOException {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstExp));
    }

}
