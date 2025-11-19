import backend.MipsGenModule;
import config.Config;
import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import ir.IRModule;
import ir.LLVMGenerator;
import pass.PassModule;
import utils.IOUtils;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Config.init();

        //词法分析
        Lexer.getInstance().analyze(IOUtils.read(Config.fileInPath));

        //根据需要设置Config里的布尔值，进行输出
        if(Config.lexer){
            Lexer.getInstance().printLexAns();
        }

        //语法分析
        Parser.getInstance().setTokens(Lexer.getInstance().getTokens());
        Parser.getInstance().analyze();
        if(Config.parser){
            Parser.getInstance().printParseAns();
        }

        //符号表输出+打印错误
        ErrorHandler.getInstance().compUnitError(Parser.getInstance().getCompUnitNode());
        if(Config.symbol){
            ErrorHandler.getInstance().printSymbolTablesFun();
        }

        if(Config.error){
            ErrorHandler.getInstance().printErrors();
        }

        //中间代码生成

        if (Config.ir) {
            LLVMGenerator.getInstance().visitCompUnit(Parser.getInstance().getCompUnitNode());
            IOUtils.llvm_ir_raw(IRModule.getInstance().toString());
            PassModule.getInstance().runIRPasses();
            IOUtils.llvm_ir(IRModule.getInstance().toString());
        }
        if (Config.mips) {
            MipsGenModule.getInstance().loadIR();
            MipsGenModule.getInstance().genMips();
        }

    }
}
