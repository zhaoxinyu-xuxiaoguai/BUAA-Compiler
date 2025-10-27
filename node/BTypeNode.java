package node;

import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class BTypeNode {
    // BType -> 'int'
    private Token token;

    public BTypeNode(Token token) {
        this.token = token;
    }

    public void print() throws IOException {
        IOUtils.write(token.toString());
    }
}
