package smybol;

import java.util.List;

public class FuncSymbol extends Symbol{
    private FuncType type;
    private List<FuncParam> funcParams;

    public FuncSymbol(String name,String symbolType,FuncType type, List<FuncParam> funcParams) {
        super(name,symbolType);
        this.type = type;
        this.funcParams = funcParams;
    }

    public FuncType getType() {
        return type;
    }

    public List<FuncParam> getFuncParams() {
        return funcParams;
    }
}
