package node;

import java.io.IOException;

public class DeclNode {
    // Decl -> ConstDecl | VarDecl
    private ConstDeclNode constDecl;
    private VarDeclNode varDecl;

    public DeclNode(ConstDeclNode constDecl, VarDeclNode varDecl) {
        this.constDecl = constDecl;
        this.varDecl = varDecl;
    }

    public ConstDeclNode getConstDecl() {
        return constDecl;
    }

    public VarDeclNode getVarDecl() {
        return varDecl;
    }

    public void print() throws IOException {
        if (constDecl != null) {
            constDecl.print();
        } else {
            varDecl.print();
        }
    }
}
