package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class InitValNode {
    // InitVal -> Exp | '{' [ Exp { ',' Exp } ] '}'
    private List<ExpNode> expNode;
    private Token leftBraceToken;
   // private List<InitValNode> initValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public InitValNode(List<ExpNode> expNode, Token leftBraceToken,List<Token> commas, Token rightBraceToken) {
        this.expNode = expNode;
        this.leftBraceToken = leftBraceToken;
        //this.initValNodes = initValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public List<ExpNode> getExpNode() {
        return expNode;
    }

//    public List<InitValNode> getInitValNodes() {
//        return initValNodes;
//    }

    public void print() throws IOException {
        if (leftBraceToken==null) {
            expNode.get(0).print();
        } else {
            IOUtils.write(leftBraceToken.toString());
            if(expNode.size()>0){
                expNode.get(0).print();
                for (int i = 1; i < expNode.size(); i++) {
                    IOUtils.write(commas.get(i-1).toString());
                    expNode.get(i).print();
                }
            }

            IOUtils.write(rightBraceToken.toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.InitVal));
    }
}
