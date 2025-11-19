package utils;

import config.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.StringJoiner;

public class IOUtils {
    public static String read(String filename) throws IOException{
        InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(filename)));
        Scanner scanner = new Scanner(in);
        StringJoiner stringJoiner = new StringJoiner("\n");
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        in.close();
        return stringJoiner.toString();
    }

    public static void write(String content,String filename) throws IOException {
        try {
            // 使用 StandardOpenOption.APPEND 来追加内容
            Files.writeString(
                    Paths.get(filename),
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,  // 如果文件不存在则创建
                    StandardOpenOption.APPEND    // 追加模式
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static void write(String content) throws IOException {
        write(content, Config.fileOutPath);
    }

    public static void llvm_ir_raw(String content) throws IOException {
        write(content, Config.fileLlvmIRRawPath);
    }

    public static void llvm_ir(String message) throws IOException {
        write(message, Config.fileLlvmIRPath);
    }

    public static void error(String msg) throws IOException {
        write(msg,Config.fileErrorPath);
    }

    public static void symbol(String msg) throws IOException{
        write(msg,Config.fileOutPath);
    }

    public static void delete(String filename){
        File file=new File(filename);
        if(file.exists()){
            file.delete();
        }
    }

    public static void mips(String message) throws IOException {
        write(message, Config.fileMipsPath);
    }

    public static void clear(String filename) throws IOException {
        delete(filename);
        write("",filename);
    }
}
