package node;

import frontend.Parser;
//import symbol.FuncType;
import smybol.FuncType;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class FuncTypeNode {
    // FuncType -> 'void' | 'int'

    private Token token;

    public FuncTypeNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void print() throws IOException {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.FuncType));
    }

    public FuncType getType() {
        if (token.getContent().equals("void")) {
            return FuncType.VOID;
        } else {
            return FuncType.INT;
        }
    }


//    public FuncType getType() {
//        if (token.getContent().equals("void")) {
//            return FuncType.VOID;
//        } else {
//            return FuncType.INT;
//        }
//    }
}
