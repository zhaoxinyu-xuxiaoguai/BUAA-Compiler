package config;

import utils.IOUtils;

import java.io.IOException;

public class Config {
    /*
    paths of files
     */
    public static boolean test = false;
    public static String fileInPath = "testfile.txt";
    public static String fileOutPath ="symbol.txt"; //"parser.txt";//
    public static String fileErrorPath =  "error.txt";
    public static String fileLlvmIRRawPath = "llvm_ir_raw.txt";
    public static String fileLlvmIRPath = "llvm_ir.txt";
    public static String fileMipsPath = "mips.txt";
    public static String stdOutPath = "stdout.txt";
    /**
     * stages of compilation
     */
    public static boolean lexer = false;
    public static boolean parser = false;
    public static boolean error = true;
    public static boolean symbol=true;
    //public static boolean ir = true;
   // public static boolean mips = true;

    public static void init() throws IOException {
        IOUtils.clear(fileOutPath);
        IOUtils.clear(fileErrorPath);
        //IOUtils.delete(fileLlvmIRRawPath);
        //IOUtils.delete(fileLlvmIRPath);
        //IOUtils.delete(fileMipsPath);
        //System.setOut(new PrintStream(stdOutPath));
    }
}
