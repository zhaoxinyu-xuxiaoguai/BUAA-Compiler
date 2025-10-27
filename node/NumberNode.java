package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class NumberNode {
    // Number -> IntConst

    Token token;

    public NumberNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void print() throws IOException {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.Number));
    }

    public String getStr() {
        return token.getContent();
    }
}
