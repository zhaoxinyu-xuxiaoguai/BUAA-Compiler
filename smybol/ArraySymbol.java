package smybol;

public class ArraySymbol extends Symbol{
    private boolean isConst;//是否是常量
    private int dimension;//0变量 1数组

    public ArraySymbol(String name,String symboleType,boolean isConst, int dimension) {
        super(name,symboleType);
        this.isConst = isConst;
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }

    public boolean isConst() {
        return isConst;
    }
}
