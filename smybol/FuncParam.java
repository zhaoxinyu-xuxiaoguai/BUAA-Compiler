package smybol;

public class FuncParam {
    private String name;
    private int dimension;

    public FuncParam(String name,int dimension) {
        this.name = name;
        this.dimension=dimension;
    }

    public String getName() {
        return name;
    }


    public Integer getDimension() {
        return dimension;
    }
}
