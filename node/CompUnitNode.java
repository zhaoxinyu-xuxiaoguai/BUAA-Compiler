package node;

import frontend.Parser;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class CompUnitNode {
    //CompUnit -> {Decl} {FuncDef} MainFuncDef
    private List<DeclNode> declNodes;
    private List<FuncDefNode> funcDefNodes;
    private MainFuncDefNode mainFuncDefNode;

    public CompUnitNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, MainFuncDefNode mainFuncDefNode) {
        this.declNodes = declNodes;
        this.funcDefNodes = funcDefNodes;
        this.mainFuncDefNode = mainFuncDefNode;
    }

    public List<DeclNode> getDeclNodes() {
        return declNodes;
    }

    public void setDeclNodes(List<DeclNode> declNodes) {
        this.declNodes = declNodes;
    }

    public List<FuncDefNode> getFuncDefNodes() {
        return funcDefNodes;
    }

    public void setFuncDefNodes(List<FuncDefNode> funcDefNodes) {
        this.funcDefNodes = funcDefNodes;
    }

    public MainFuncDefNode getMainFuncDefNode() {
        return mainFuncDefNode;
    }

    public void setMainFuncDefNode(MainFuncDefNode mainFuncDefNode) {
        this.mainFuncDefNode = mainFuncDefNode;
    }

    //print最后统一写
    public void print() throws IOException {
        for(DeclNode declNode:declNodes){
            declNode.print();
        }
        for(FuncDefNode funcDefNode:funcDefNodes){
            funcDefNode.print();
        }
        mainFuncDefNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.CompUnit));
    }
}
