package smybol;

import error.Error;

import java.lang.reflect.Type;

public class Symbol{
    private String name;
    private String SymbolType;

    public Symbol(String name, String symbolType) {

        this.name = name;
        SymbolType = symbolType;
    }




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbolType() {
        return SymbolType;
    }

    public void setSymbolType(String symbolType) {
        SymbolType = symbolType;
    }

    @Override
    public String toString() {
        return name+' '+SymbolType+'\n';
    }
}
