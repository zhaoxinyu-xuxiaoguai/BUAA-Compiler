package ir;

import config.Config;
import ir.types.*;
import ir.values.*;
import ir.values.instructions.ConstArray;
import ir.values.instructions.Operator;
import node.*;
import token.TokenType;
import utils.Pair;

import java.util.*;

public class LLVMGenerator {
    private static final LLVMGenerator instance = new LLVMGenerator();

    public static LLVMGenerator getInstance() {
        return instance;
    }

    private BuildFactory buildFactory = BuildFactory.getInstance();//统一创建 IR 实体（函数、基本块、指令、类型）的工厂。

    // Map to track static variable names and ensure uniqueness
    private Map<String, Integer> staticVarCounter = new HashMap<>();

    private BasicBlock curBlock = null;//处理布尔短路与 if/while 条件跳转时的真分支/假分支目标块。
    private BasicBlock curTrueBlock = null;//while/for 中遇到 continue 时跳转的块（通常是下一次判断块）。
    private BasicBlock curFalseBlock = null;//同上
    private BasicBlock continueBlock = null;//while/for 中遇到 continue 时跳转的块（通常是下一次判断块）。
    private BasicBlock curWhileFinalBlock = null;// while 循环的退出块（break 的跳转目标）。
    private Function curFunction = null;//当前正在生成的函数。

    /**
     * 计算时需要保留的
     */
    private Integer saveValue = null;//常量折叠模式下保存“当前子表达式”的整数值。
    private Operator saveOp = null;//常量折叠时暂存待应用的二元运算符（Add/Sub/Mul/Div/Mod）。
    private int tmpIndex = 0;//处理形参寄存区映射时的临时下标（如注册形参到局部变量）。
    private Operator tmpOp = null;//处理临时运算符（如二元运算符的优先级计算）。
    private Type tmpType = null;//处理临时类型（如数组元素类型）。
    private Value tmpValue = null;//处理临时值（如临时计算结果）。
    private List<Value> tmpList = null;//处理临时列表（如多维数组索引计算）。
    private List<Type> tmpTypeList = null;//处理临时类型列表（如多维数组类型）。
    private List<Value> funcArgsList = null;//处理函数参数列表（如注册形参到局部变量）。

    private boolean isGlobal = true;//是否在全局作用域（如全局变量定义）。
    private boolean isConst = false;//是否在常量定义（如常量初始化）。
    private boolean isArray = false;//是否在数组定义（如数组初始化）。
    private boolean isRegister = false;//是否在寄存器分配（如寄存器分配）。
    private boolean isStatic = false;//是否在static变量定义（如static局部变量）。

    /**
     * 数组相关
     */
    private Value curArray = null;//当前处理的数组（如多维数组索引计算）。
    private String tmpName = null;//当前处理的临时名称（如临时变量名）。
    private int tmpDepth = 0;//当前处理的临时深度（如多维数组深度）。
    private int tmpOffset = 0;//当前处理的临时偏移量（如多维数组偏移量）。
    private List<Integer> tmpDims = null;//当前处理的临时维度（如多维数组维度）。


    /**
     * 新符号表系统和常量表
     */
    private List<Pair<Map<String, Value>, Map<String, Integer>>> symbolTable = new ArrayList<>();

    private Map<String, Value> getCurSymbolTable() {
        return symbolTable.get(symbolTable.size() - 1).getFirst();
    }

    private void addSymbol(String name, Value value) {
        getCurSymbolTable().put(name, value);
    }

    private void addGlobalSymbol(String name, Value value) {
        symbolTable.get(0).getFirst().put(name, value);
    }

    private Value getValue(String name) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getFirst().containsKey(name)) {
                return symbolTable.get(i).getFirst().get(name);
            }
        }
        return null;
    }

    /**
     * 常量表
     */

    private Map<String, Integer> getCurConstTable() {
        return symbolTable.get(symbolTable.size() - 1).getSecond();
    }

    private void addConst(String name, Integer value) {
        getCurConstTable().put(name, value);
    }

    private Integer getConst(String name) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getSecond().containsKey(name)) {
                return symbolTable.get(i).getSecond().get(name);
            }
        }
        return 0;
    }

    private void changeConst(String name, Integer value) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getSecond().containsKey(name)) {
                symbolTable.get(i).getSecond().put(name, value);
                return;
            }
        }
    }

    /**
     * 添加和删除当前块符号表和常量表
     */
    private void addSymbolAndConstTable() {
        symbolTable.add(new Pair<>(new HashMap<>(), new HashMap<>()));
    }

    private void removeSymbolAndConstTable() {
        symbolTable.remove(symbolTable.size() - 1);
    }

    private int calculate(Operator op, int a, int b) {
        switch (op) {
            case Add:
                return a + b;
            case Sub:
                return a - b;
            case Mul:
                return a * b;
            case Div:
                return a / b;
            case Mod:
                return a % b;
            default:
                return 0;
        }
    }

    /**
     * 字符串相关
     */
    private List<String> stringList = new ArrayList<>();

    private int getStringIndex(String str) {
        for (int i = 0; i < stringList.size(); i++) {
            if (stringList.get(i).equals(str)) {
                return i;
            }
        }
        stringList.add(str);
        Type type = buildFactory.getArrayType(IntegerType.i8, str.length() + 1);
        Value value = buildFactory.buildGlobalVar(getStringName(str), type, true, buildFactory.getConstString(str));
        addGlobalSymbol(getStringName(str), value);
        return stringList.size() - 1;
    }

    private String getStringName(int index) {
        return "_str_" + index;
    }

    private String getStringName(String str) {
        return getStringName(getStringIndex(str));
    }

    /**
     * 遍历语法树
     */
    public void visitCompUnit(CompUnitNode compUnitNode) {
        addSymbolAndConstTable();
        addSymbol("getint", buildFactory.buildLibraryFunction("getint", IntegerType.i32, new ArrayList<>()));
        addSymbol("putint", buildFactory.buildLibraryFunction("putint", VoidType.voidType, new ArrayList<>(Collections.singleton(IntegerType.i32))));
        addSymbol("putch", buildFactory.buildLibraryFunction("putch", VoidType.voidType, new ArrayList<>(Collections.singleton(IntegerType.i32))));
        addSymbol("putstr", buildFactory.buildLibraryFunction("putstr", VoidType.voidType, new ArrayList<>(Collections.singleton(new PointerType(IntegerType.i8)))));

        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            visitDecl(declNode);
        }
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            visitFuncDef(funcDefNode);
        }
        visitMainFuncDef(compUnitNode.getMainFuncDefNode());
    }

    private void visitDecl(DeclNode declNode) {
        // Decl -> ConstDecl | VarDecl
        if (declNode.getConstDecl() != null) {
            visitConstDecl(declNode.getConstDecl());
        } else {
            visitVarDecl(declNode.getVarDecl());
        }
    }

    private void visitConstDecl(ConstDeclNode constDeclNode) {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        tmpType = IntegerType.i32;
        for (ConstDefNode constDefNode : constDeclNode.getConstDefNodeList()) {
            visitConstDef(constDefNode);
        }
    }

    private void visitConstDef(ConstDefNode constDefNode) {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        String name = constDefNode.getIdent().getContent();
        if (constDefNode.getConstExpNodes().isEmpty()) {
            // is not an array
            visitConstInitVal(constDefNode.getConstInitValNode());
            //saveValue：之前通过 visitConstInitVal 计算出的常量值（整数）
            tmpValue = buildFactory.getConstInt(saveValue == null ? 0 : saveValue);
            addConst(name, saveValue);
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalVar(name, tmpType, true, tmpValue);
                addSymbol(name, tmpValue);
            } else {
                tmpValue = buildFactory.buildVar(curBlock, tmpValue, true, tmpType);
                addSymbol(name, tmpValue);
            }
        } else {
            // beauty里考虑了多维数组的情况
            // is an array
            // List<Integer> dims = new ArrayList<>();
            // for (ConstExpNode constExpNode : constDefNode.getConstExpNodes()) {
            //     visitConstExp(constExpNode);
            //     dims.add(saveValue);
            // }
            // tmpDims = new ArrayList<>(dims);
            // Type type = null;
            // for (int i = dims.size() - 1; i >= 0; i--) {
            //     if (type == null) {
            //         type = buildFactory.getArrayType(tmpType, dims.get(i));
            //     } else {
            //         type = buildFactory.getArrayType(type, dims.get(i));
            //     }
            // }
            // if (isGlobal) {
            //     tmpValue = buildFactory.buildGlobalArray(name, type, true);
            //     ((ConstArray) ((GlobalVar) tmpValue).getValue()).setInit(true);
            // } else {
            //     tmpValue = buildFactory.buildArray(curBlock, true, type);
            // }
            // addSymbol(name, tmpValue);
            // curArray = tmpValue;
            // isArray = true;
            // tmpName = name;
            // tmpDepth = 0;
            // tmpOffset = 0;
            // visitConstInitVal(constDefNode.getConstInitValNode());
            // isArray = false;
            // is an array (1D only)
            // 计算数组大小
         
            visitConstExp(constDefNode.getConstExpNodes().get(0));
            int arraySize = saveValue;//递归得到数组大小
    

            // 构建一维数组类型
            Type type = buildFactory.getArrayType(tmpType, arraySize);
            tmpDims = new ArrayList<>();
            tmpDims.add(arraySize);

            // 创建数组变量
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalArray(name, type, true);
                ((ConstArray) ((GlobalVar) tmpValue).getValue()).setInit(true);
            } else {
                tmpValue = buildFactory.buildArray(curBlock, true, type);
            }
            addSymbol(name, tmpValue);

            // 设置初始化上下文
            curArray = tmpValue;
            isArray = true;
            tmpName = name;
            tmpDepth = 0;
            tmpOffset = 0;
            visitConstInitVal(constDefNode.getConstInitValNode());
            isArray = false;
        }
    }

 private void visitConstInitVal(ConstInitValNode constInitValNode) {
      // ConstInitVal -> ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'

      // 简单常量初始化（非数组）
      if (!isArray) {
          if (constInitValNode.getConstExpNode() != null
              && !constInitValNode.getConstExpNode().isEmpty()) {
              visitConstExp(constInitValNode.getConstExpNode().get(0));
          }
          return;
      }

      // 一维数组初始化
      if (constInitValNode.getConstExpNode() == null
          || constInitValNode.getConstExpNode().isEmpty()) {
          return;  // 无初始值
      }

      // 统一处理所有元素（有无花括号都一样）
      for (ConstExpNode constExpNode : constInitValNode.getConstExpNode()) {
          // 1. 计算常量值
          visitConstExp(constExpNode);
          tmpValue = buildFactory.getConstInt(saveValue);

          // 2. 存储到数组
          if (isGlobal) {
              buildFactory.buildInitArray(curArray, tmpOffset, tmpValue);
          } else {
              buildFactory.buildStore(curBlock,
                  buildFactory.buildGEP(curBlock, curArray, tmpOffset), tmpValue);
          }

          // 3. 更新常量表（简化版）
          addConst(tmpName + tmpOffset + ";", saveValue);

          // 4. 移动到下一个位置
          tmpOffset++;
      }
  }

    private void visitVarDecl(VarDeclNode varDeclNode) {
        // VarDecl -> [ 'static' ] BType VarDef { ',' VarDef } ';'
        tmpType = IntegerType.i32;
        // If static keyword is present, mark as static (but don't change isGlobal)
        if (varDeclNode.getStaticToken() != null) {
            isStatic = true;
        }
        for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
            visitVarDef(varDefNode);
        }
        // Restore static flag
        isStatic = false;
    }

    private void visitVarDef(VarDefNode varDefNode) {
        // VarDef -> Ident ['[' ConstExp ']'] [ '=' InitVal ]
        String name = varDefNode.getIdent().getContent();
        String mangledName = name; // Name used in LLVM IR

        // If this is a static local variable (not in global scope), mangle the name
        if (isStatic && !isGlobal) {
            // Generate mangled name: functionName.varName with unique counter
            String baseName = curFunction.getName() + "." + name;

            // Check if this name has been used before
            if (staticVarCounter.containsKey(baseName)) {
                // Name exists, append counter
                int counter = staticVarCounter.get(baseName);
                mangledName = baseName + "." + counter;
                staticVarCounter.put(baseName, counter + 1);
            } else {
                // First time using this name
                mangledName = baseName;
                staticVarCounter.put(baseName, 1);
            }
        }

        if (varDefNode.getConstExpNodes() == null) {
            // is not an array
            if (varDefNode.getInitValNode() != null) {
                tmpValue = null;
                if (isGlobal || isStatic) {
                    //开启常量折叠
                    isConst = true;
                    saveValue = null;
                }
                visitInitVal(varDefNode.getInitValNode());
                isConst = false;
            } else {
                tmpValue = null;
                if (isGlobal || isStatic) {
                    saveValue = null;
                }
            }
            if (isGlobal || isStatic) {
                // Use mangled name for LLVM IR generation
                tmpValue = buildFactory.buildGlobalVar(mangledName, tmpType, false, buildFactory.getConstInt(saveValue == null ? 0 : saveValue));
                // Add to symbol table with original name so it can be accessed in the function
                addSymbol(name, tmpValue);
            } else {
                tmpValue = buildFactory.buildVar(curBlock, tmpValue, isConst, tmpType);
                addSymbol(name, tmpValue);
            }
        } else {
            // is an array (only 1D supported in this grammar)
            isConst = true;
            visitConstExp(varDefNode.getConstExpNodes());
            int dim = saveValue;
            isConst = false;
            Type type = buildFactory.getArrayType(tmpType, dim);
            tmpDims = new ArrayList<>();
            tmpDims.add(dim);

            if (isGlobal || isStatic) {
                // Use mangled name for LLVM IR generation
                tmpValue = buildFactory.buildGlobalArray(mangledName, type, false);
                if (varDefNode.getInitValNode() != null) {
                    ((ConstArray) ((GlobalVar) tmpValue).getValue()).setInit(true);
                }
                // Add to symbol table with original name
                addSymbol(name, tmpValue);
            } else {
                tmpValue = buildFactory.buildArray(curBlock, false, type);
                addSymbol(name, tmpValue);
            }
            curArray = tmpValue;
            if (varDefNode.getInitValNode() != null) {
                isArray = true;
                tmpName = name;
                tmpDepth = 0;
                tmpOffset = 0;
                visitInitVal(varDefNode.getInitValNode());
                isArray = false;
            }
            isConst = false;
        }
    }

    private void visitInitVal(InitValNode initValNode) {
        // InitVal -> Exp | '{' [ Exp { ',' Exp } ] '}'
        // Note: This grammar doesn't support nested InitVal, only a list of Exp
        if (initValNode.getExpNode() != null && !isArray) {
            // Single Exp (not in array context)
            if (!initValNode.getExpNode().isEmpty()) {
                visitExp(initValNode.getExpNode().get(0));
            }
        } else if (isArray) {
            // Array initialization
            if (initValNode.getExpNode() != null && !initValNode.getExpNode().isEmpty()) {
                // Process all expressions in the list
                for (ExpNode expNode : initValNode.getExpNode()) {
                    if (isGlobal) {
                        isConst = true;
                    }
                    saveValue = null;
                    tmpValue = null;
                    visitExp(expNode);
                    isConst = false;
                    if (isGlobal) {
                        tmpValue = buildFactory.getConstInt(saveValue);
                        buildFactory.buildInitArray(curArray, tmpOffset, tmpValue);
                    } else {
                        buildFactory.buildStore(curBlock, buildFactory.buildGEP(curBlock, curArray, tmpOffset), tmpValue);
                    }
                    tmpOffset++;
                }
                tmpDepth = 1;
            }
        }
    }

    private void visitFuncDef(FuncDefNode funcDefNode) {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        isGlobal = false;
        String funcName = funcDefNode.getIdent().getContent();
        Type type = funcDefNode.getFuncTypeNode().getToken().getType() == TokenType.INTTK ? IntegerType.i32 : VoidType.voidType;
        tmpTypeList = new ArrayList<>();
        if (funcDefNode.getFuncFParamsNode() != null) {
            visitFuncFParams(funcDefNode.getFuncFParamsNode());
        }
        Function function = buildFactory.buildFunction(funcName, type, tmpTypeList);
        curFunction = function;
        addSymbol(funcName, function);
        addSymbolAndConstTable();
        addSymbol(funcName, function);
        curBlock = buildFactory.buildBasicBlock(curFunction);
        funcArgsList = buildFactory.getFunctionArguments(curFunction);
        isRegister = true;
        if (funcDefNode.getFuncFParamsNode() != null) {
            visitFuncFParams(funcDefNode.getFuncFParamsNode());
        }
        isRegister = false;
        visitBlock(funcDefNode.getBlockNode());
        isGlobal = true;
        removeSymbolAndConstTable();
        buildFactory.checkBlockEnd(curBlock);
    }

    private void visitMainFuncDef(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        isGlobal = false;
        Function function = buildFactory.buildFunction("main", IntegerType.i32, new ArrayList<>());
        curFunction = function;
        addSymbol("main", function);
        addSymbolAndConstTable();
        addSymbol("main", function);
        curBlock = buildFactory.buildBasicBlock(curFunction);
        funcArgsList = buildFactory.getFunctionArguments(curFunction);
        visitBlock(mainFuncDefNode.getBlockNode());
        isGlobal = true;
        removeSymbolAndConstTable();
        buildFactory.checkBlockEnd(curBlock);
    }

    private void visitFuncFParams(FuncFParamsNode funcFParamsNode) {
        // FuncFParams -> FuncFParam { ',' FuncFParam }
        if (isRegister) {
            tmpIndex = 0;
            for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
                visitFuncFParam(funcFParamNode);
                tmpIndex++;
            }
        } else {
            tmpTypeList = new ArrayList<>();
            for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
                visitFuncFParam(funcFParamNode);
                tmpTypeList.add(tmpType);
            }
        }
    }

    private void visitFuncFParam(FuncFParamNode funcFParamNode) {
        // BType Ident [ '[' ']' ]
        if (isRegister) {
            int i = tmpIndex;
            Value value = buildFactory.buildVar(curBlock, funcArgsList.get(i), false, tmpTypeList.get(i));
            addSymbol(funcFParamNode.getIdent().getContent(), value);
        } else {
            if (funcFParamNode.getLeftBrackets() == null) {
                // not an array parameter
                tmpType = IntegerType.i32;
            } else {
                // array parameter int arr[]
                tmpType = buildFactory.getArrayType(IntegerType.i32, -1);
            }
        }
    }

    private void visitBlock(BlockNode blockNode) {
        // Block -> '{' { BlockItem } '}'
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            visitBlockItem(blockItemNode);
        }
    }

    private void visitBlockItem(BlockItemNode blockItemNode) {
        // BlockItem -> Decl | Stmt
        if (blockItemNode.getDeclNode() != null) {
            visitDecl(blockItemNode.getDeclNode());
        } else {
            visitStmt(blockItemNode.getStmtNode());
        }
    }

    private void visitStmt(StmtNode stmtNode) {
        // Stmt -> LVal '=' Exp ';'
        //	| [Exp] ';'
        //	| Block
        //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        //	| 'while' '(' Cond ')' Stmt
        //	| 'break' ';' | 'continue' ';'
        //	| 'return' [Exp] ';'
        //	| LVal '=' 'getint' '(' ')' ';'
        //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
        switch (stmtNode.getType()) {
            case LValAssignExp:
                if (stmtNode.getLValNode().getExpNodes().isEmpty()) {
                    // is not an array
                    Value input = getValue(stmtNode.getLValNode().getIdent().getContent());
                    visitExp(stmtNode.getExpNode());
                    tmpValue = buildFactory.buildStore(curBlock, input, tmpValue);
                } else {
                    // is an array (一维数组)
                    // 计算数组下标
                    visitExp(stmtNode.getLValNode().getExpNodes().get(0));
                    Value index = tmpValue;

                    // 获取数组地址
                    tmpValue = getValue(stmtNode.getLValNode().getIdent().getContent());
                    Value addr;
                    Type targetType = ((PointerType) tmpValue.getType()).getTargetType();

                    if (targetType instanceof PointerType) {
                        // 函数形参数组: int arr[]
                        // 类型: i32** -> 需要先 load 出 i32*
                        tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                        addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(index));
                    } else {
                        // 局部/全局数组: int arr[10]
                        // 类型: [10 x i32]* -> 需要添加首索引 0
                        addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(ConstInt.ZERO, index));
                    }

                    // 计算右侧表达式并存储
                    visitExp(stmtNode.getExpNode());
                    tmpValue = buildFactory.buildStore(curBlock, addr, tmpValue);
                }
                break;
            case Exp:
                if (stmtNode.getExpNode() != null) {
                    visitExp(stmtNode.getExpNode());
                }
                break;
            case Block:
                addSymbolAndConstTable();
                visitBlock(stmtNode.getBlockNode());
                removeSymbolAndConstTable();
                break;
            case If:
                if (stmtNode.getElseToken() == null) {
                    // basicBlock;
                    // if (...) {
                    //    trueBlock;
                    // }
                    // finalBlock;
                    BasicBlock basicBlock = curBlock;

                    BasicBlock trueBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = trueBlock;
                    visitStmt(stmtNode.getStmtNodes().get(0));
                    BasicBlock finalBlock = buildFactory.buildBasicBlock(curFunction);
                    buildFactory.buildBr(curBlock, finalBlock);

                    curTrueBlock = trueBlock;
                    curFalseBlock = finalBlock;
                    curBlock = basicBlock;
                    visitCond(stmtNode.getCondNode());

                    curBlock = finalBlock;
                } else {
                    // basicBlock;
                    // if (...) {
                    //    trueBlock;
                    //    ...
                    //    trueEndBlock;
                    // } else {
                    //    falseBlock;
                    //    ...
                    //    falseEndBlock;
                    // }
                    // finalBlock;
                    BasicBlock basicBlock = curBlock;

                    BasicBlock trueBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = trueBlock;
                    visitStmt(stmtNode.getStmtNodes().get(0));
                    BasicBlock trueEndBlock = curBlock;

                    BasicBlock falseBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = falseBlock;
                    visitStmt(stmtNode.getStmtNodes().get(1));
                    BasicBlock falseEndBlock = curBlock;

                    curBlock = basicBlock;
                    curTrueBlock = trueBlock;
                    curFalseBlock = falseBlock;
                    visitCond(stmtNode.getCondNode());

                    BasicBlock finalBlock = buildFactory.buildBasicBlock(curFunction);
                    buildFactory.buildBr(trueEndBlock, finalBlock);
                    buildFactory.buildBr(falseEndBlock, finalBlock);
                    curBlock = finalBlock;
                }
                break;
            case For:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                // basicBlock;
                // [forStmt1]; // initialization
                // goto judgeBlock;
                //
                // judgeBlock:
                // if (cond) {
                //    goto bodyBlock;
                // } else {
                //    goto finalBlock;
                // }
                //
                // bodyBlock:
                // stmtBody;
                // goto updateBlock;
                //
                // updateBlock:
                // [forStmt2]; // update
                // goto judgeBlock;
                //
                // finalBlock:

                BasicBlock forBasicBlock = curBlock;
                BasicBlock tmpContinueBlock2 = continueBlock;
                BasicBlock tmpWhileFinalBlock2 = curWhileFinalBlock;

                // Execute ForStmt1 (initialization) in current block
                if (stmtNode.getForStmtNode1() != null) {
                    visitForStmt(stmtNode.getForStmtNode1());
                }

                BasicBlock forJudgeBlock = buildFactory.buildBasicBlock(curFunction);
                buildFactory.buildBr(forBasicBlock, forJudgeBlock);

                BasicBlock forBodyBlock = buildFactory.buildBasicBlock(curFunction);
                BasicBlock forUpdateBlock = buildFactory.buildBasicBlock(curFunction);
                BasicBlock forFinalBlock = buildFactory.buildBasicBlock(curFunction);

                // Set continue to jump to update block
                continueBlock = forUpdateBlock;
                curWhileFinalBlock = forFinalBlock;

                // Generate body
                curBlock = forBodyBlock;
                visitStmt(stmtNode.getStmtNodes().get(0));
                buildFactory.buildBr(curBlock, forUpdateBlock);

                // Generate update block
                curBlock = forUpdateBlock;
                if (stmtNode.getForStmtNode2() != null) {
                    visitForStmt(stmtNode.getForStmtNode2());
                }
                buildFactory.buildBr(curBlock, forJudgeBlock);

                // Generate judge block
                curBlock = forJudgeBlock;
                curTrueBlock = forBodyBlock;
                curFalseBlock = forFinalBlock;
                if (stmtNode.getCondNode() != null) {
                    visitCond(stmtNode.getCondNode());
                } else {
                    // No condition means always true
                    buildFactory.buildBr(curBlock, forBodyBlock);
                }

                // Restore and move to final block
                continueBlock = tmpContinueBlock2;
                curWhileFinalBlock = tmpWhileFinalBlock2;
                curBlock = forFinalBlock;
                break;
            case Break:
                buildFactory.buildBr(curBlock, curWhileFinalBlock);
                break;
            case Continue:
                buildFactory.buildBr(curBlock, continueBlock);
                break;
            case Return:
                if (stmtNode.getExpNode() == null) {
                    buildFactory.buildRet(curBlock);
                } else {
                    visitExp(stmtNode.getExpNode());
                    buildFactory.buildRet(curBlock, tmpValue);
                }
                break;
            case Printf:
                String formatStrings = stmtNode.getFormatString().getContent().replace("\\n", "\n").replace("\"", "");
                List<Value> args = new ArrayList<>();
                for (ExpNode expNode : stmtNode.getExpNodes()) {
                    visitExp(expNode);
                    args.add(tmpValue);
                }
                for (int i = 0; i < formatStrings.length(); i++) {
                    if (formatStrings.charAt(i) == '%') {
                        buildFactory.buildCall(curBlock, (Function) getValue("putint"), new ArrayList<Value>() {{
                            add(args.remove(0));
                        }});
                        i++;
                    } else {
                        if (Config.chToStr) {
                            int j = i;
                            while (j < formatStrings.length() && formatStrings.charAt(j) != '%') {
                                j++;
                            }
                            String str = formatStrings.substring(i, j);
                            if (str.length() == 1) {
                                buildFactory.buildCall(curBlock, (Function) getValue("putch"), new ArrayList<Value>() {{
                                    add(buildFactory.getConstInt(str.charAt(0)));
                                }});
                            } else {
                                Value strAddr = buildFactory.buildGEP(curBlock, getValue(getStringName(str)), new ArrayList<Value>() {{
                                    add(ConstInt.ZERO);
                                    add(ConstInt.ZERO);
                                }});
                                buildFactory.buildCall(curBlock, (Function) getValue("putstr"), new ArrayList<Value>() {{
                                    add(strAddr);
                                }});
                                i = j - 1;
                            }
                        } else {
                            int finalI = i;
                            buildFactory.buildCall(curBlock, (Function) getValue("putch"), new ArrayList<Value>() {{
                                add(buildFactory.getConstInt(formatStrings.charAt(finalI)));
                            }});
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("Unknown StmtNode type: " + stmtNode.getType());
        }
    }

    private void visitExp(ExpNode expNode) {
        // Exp -> AddExp
        tmpValue = null;
        saveValue = null;
        visitAddExp(expNode.getAddExpNode());
    }

    private void visitCond(CondNode condNode) {
        // Cond -> LOrExp
        visitLOrExp(condNode.getLOrExpNode());
    }

    private void visitLVal(LValNode lValNode) {
        // LVal -> Ident {'[' Exp ']'}
        if (isConst) {
            // 常量上下文：获取常量值
            StringBuilder name = new StringBuilder(lValNode.getIdent().getContent());
            if (!lValNode.getExpNodes().isEmpty()) {
                // 常量数组元素：arr[index]
                name.append("0;");
                visitExp(lValNode.getExpNodes().get(0));
                name.append(buildFactory.getConstInt(saveValue == null ? 0 : saveValue).getValue()).append(";");
            }
            saveValue = getConst(name.toString());
        } else {
            // 运行时上下文
            if (lValNode.getExpNodes().isEmpty()) {
                // 无下标：普通变量或数组名
                Value addr = getValue(lValNode.getIdent().getContent());
                Type targetType = ((PointerType) addr.getType()).getTargetType();

                if (targetType instanceof ArrayType) {
                    // 数组名：返回首元素地址（用于函数参数传递等）
                    tmpValue = buildFactory.buildGEP(curBlock, addr, Arrays.asList(ConstInt.ZERO, ConstInt.ZERO));
                } else {
                    // 普通变量：load 值
                    tmpValue = buildFactory.buildLoad(curBlock, addr);
                }
            } else {
                // 有下标：一维数组元素访问 arr[index]
                // 计算下标
                visitExp(lValNode.getExpNodes().get(0));
                Value index = tmpValue;

                // 获取数组地址
                tmpValue = getValue(lValNode.getIdent().getContent());
                Type targetType = ((PointerType) tmpValue.getType()).getTargetType();
                Value addr;

                if (targetType instanceof PointerType) {
                    // 函数形参数组: int arr[]
                    // 类型: i32** -> 需要先 load 出 i32*
                    tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                    addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(index));
                } else {
                    // 局部/全局数组: int arr[10]
                    // 类型: [10 x i32]* -> 需要添加首索引 0
                    addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(ConstInt.ZERO, index));
                }

                // load 数组元素的值
                tmpValue = buildFactory.buildLoad(curBlock, addr);
            }
        }
    }

    private void visitPrimaryExp(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            visitExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            visitLVal(primaryExpNode.getLValNode());
        } else {
            visitNumber(primaryExpNode.getNumberNode());
        }
    }

    private void visitNumber(NumberNode numberNode) {
        // Number -> IntConst
        if (isConst) {
            saveValue = Integer.parseInt(numberNode.getToken().getContent());
        } else {
            tmpValue = buildFactory.getConstInt(Integer.parseInt(numberNode.getToken().getContent()));
        }
    }


    private void visitUnaryExp(UnaryExpNode unaryExpNode) {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (unaryExpNode.getPrimaryExpNode() != null) {
            visitPrimaryExp(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getIdent() != null) {
            // Ident '(' [FuncRParams] ')'
            tmpList = new ArrayList<>();
            if (unaryExpNode.getFuncRParamsNode() != null) {
                visitFuncRParams(unaryExpNode.getFuncRParamsNode());
            }
            tmpValue = buildFactory.buildCall(curBlock, (Function) getValue(unaryExpNode.getIdent().getContent()), tmpList);
        } else {
            // UnaryOp UnaryExp
            // UnaryOp 直接在这里处理即可
            if (unaryExpNode.getUnaryOpNode().getToken().getType() == TokenType.PLUS) {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
            } else if (unaryExpNode.getUnaryOpNode().getToken().getType() == TokenType.MINU) {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
                if (isConst) {
                    saveValue = -saveValue;
                } else {
                    tmpValue = buildFactory.buildBinary(curBlock, Operator.Sub, ConstInt.ZERO, tmpValue);
                }
            } else {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
                tmpValue = buildFactory.buildNot(curBlock, tmpValue);
            }
        }
    }

    private void visitFuncRParams(FuncRParamsNode funcRParamsNode) {
        // FuncRParams -> Exp { ',' Exp }
        List<Value> args = new ArrayList<>();
        for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
            visitExp(expNode);
            args.add(tmpValue);
        }
        tmpList = args;
    }


    private void visitMulExp(MulExpNode mulExpNode) {
        // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        // Note: Parser builds tree as: MulExpNode(rightUnary, op, leftMulExp)
        // For a*b*c: MulExpNode(c, *, MulExpNode(b, *, MulExpNode(a, null, null)))
        // Need to process LEFT subtree first, then current unaryExp
        if (isConst) {
            // First process left subtree if exists
            if (mulExpNode.getMulExpNode() != null) {
                visitMulExp(mulExpNode.getMulExpNode());
                int leftValue = saveValue;
                // Then process current unaryExp
                visitUnaryExp(mulExpNode.getUnaryExpNode());
                int rightValue = saveValue;
                // Apply operator
                Operator op;
                switch (mulExpNode.getOperator().getType()) {
                    case MULT:
                        op = Operator.Mul;
                        break;
                    case DIV:
                        op = Operator.Div;
                        break;
                    case MOD:
                        op = Operator.Mod;
                        break;
                    default:
                        throw new RuntimeException("unknown operator");
                }
                saveValue = calculate(op, leftValue, rightValue);
            } else {
                // Leaf node: just a UnaryExp
                visitUnaryExp(mulExpNode.getUnaryExpNode());
            }
        } else {
            // First process left subtree if exists
            if (mulExpNode.getMulExpNode() != null) {
                visitMulExp(mulExpNode.getMulExpNode());
                Value leftValue = tmpValue;
                // Then process current unaryExp
                visitUnaryExp(mulExpNode.getUnaryExpNode());
                Value rightValue = tmpValue;
                // Apply operator
                Operator op;
                if (mulExpNode.getOperator().getType() == TokenType.MULT) {
                    op = Operator.Mul;
                } else if (mulExpNode.getOperator().getType() == TokenType.DIV) {
                    op = Operator.Div;
                } else {
                    op = Operator.Mod;
                }
                tmpValue = buildFactory.buildBinary(curBlock, op, leftValue, rightValue);
            } else {
                // Leaf node: just a UnaryExp
                visitUnaryExp(mulExpNode.getUnaryExpNode());
            }
        }
    }

    private void visitAddExp(AddExpNode addExpNode) {
        // AddExp -> MulExp | AddExp ('+' | '−') MulExp
        // Note: Parser builds tree as: AddExpNode(leftAddExp, rightMulExp, op)
        // For a+b+c: AddExpNode(AddExpNode(..., b, +), c, +)
        // Need to process LEFT subtree first, then current mulExp
        if (isConst) {
            // First process left subtree if exists
            if (addExpNode.getAddExpNode() != null) {
                visitAddExp(addExpNode.getAddExpNode());
                int leftValue = saveValue;
                // Then process current mulExp
                visitMulExp(addExpNode.getMulExpNode());
                int rightValue = saveValue;
                // Apply operator
                Operator op = addExpNode.getOperator().getType() == TokenType.PLUS ? Operator.Add : Operator.Sub;
                saveValue = calculate(op, leftValue, rightValue);
            } else {
                // Leaf node: just a MulExp
                visitMulExp(addExpNode.getMulExpNode());
            }
        } else {
            // First process left subtree if exists
            if (addExpNode.getAddExpNode() != null) {
                visitAddExp(addExpNode.getAddExpNode());
                Value leftValue = tmpValue;
                // Then process current mulExp
                visitMulExp(addExpNode.getMulExpNode());
                Value rightValue = tmpValue;
                // Apply operator
                Operator op = addExpNode.getOperator().getType() == TokenType.PLUS ? Operator.Add : Operator.Sub;
                tmpValue = buildFactory.buildBinary(curBlock, op, leftValue, rightValue);
            } else {
                // Leaf node: just a MulExp
                visitMulExp(addExpNode.getMulExpNode());
            }
        }
    }

    private void visitRelExp(RelExpNode relExpNode) {
        // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        // Note: Parser builds tree as: RelExpNode(rightAddExp, op, leftRelExp)
        // Need to process LEFT subtree first, then current addExp
        if (relExpNode.getRelExpNode() != null) {
            // Has left subtree
            visitRelExp(relExpNode.getRelExpNode());
            Value leftValue = tmpValue;
            // Then process current addExp
            visitAddExp(relExpNode.getAddExpNode());
            Value rightValue = tmpValue;
            // Apply operator
            Operator op;
            switch (relExpNode.getOperator().getType()) {
                case LSS:
                    op = Operator.Lt;
                    break;
                case LEQ:
                    op = Operator.Le;
                    break;
                case GRE:
                    op = Operator.Gt;
                    break;
                case GEQ:
                    op = Operator.Ge;
                    break;
                default:
                    throw new RuntimeException("Unknown operator");
            }
            tmpValue = buildFactory.buildBinary(curBlock, op, leftValue, rightValue);
        } else {
            // Leaf node: just an AddExp
            visitAddExp(relExpNode.getAddExpNode());
        }
    }

    private void visitEqExp(EqExpNode eqExpNode) {
        // EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        // Note: Parser builds tree as: EqExpNode(rightRelExp, op, leftEqExp)
        // Need to process LEFT subtree first, then current relExp
        if (eqExpNode.getEqExpNode() != null) {
            // Has left subtree
            visitEqExp(eqExpNode.getEqExpNode());
            Value leftValue = tmpValue;
            // Then process current relExp
            visitRelExp(eqExpNode.getRelExpNode());
            Value rightValue = tmpValue;
            // Apply operator
            Operator op = eqExpNode.getOperator().getType() == TokenType.EQL ? Operator.Eq : Operator.Ne;
            tmpValue = buildFactory.buildBinary(curBlock, op, leftValue, rightValue);
        } else {
            // Leaf node: just a RelExp
            visitRelExp(eqExpNode.getRelExpNode());
        }
    }

    private void visitLAndExp(LAndExpNode lAndExpNode) {
        // LAndExp -> EqExp | LAndExp '&&' EqExp
        // Note: Parser builds this as LAndExpNode(rightEqExp, op, leftLAndExp)
        // So we need to process LEFT (getLAndExpNode) first, then RIGHT (getEqExpNode)
        BasicBlock trueBlock = curTrueBlock;
        BasicBlock falseBlock = curFalseBlock;

        if (lAndExpNode.getLAndExpNode() != null) {
            // Has left side: first evaluate left LAndExp
            BasicBlock thenBlock = buildFactory.buildBasicBlock(curFunction);
            curTrueBlock = thenBlock;
            curFalseBlock = falseBlock;

            // Visit left side first
            visitLAndExp(lAndExpNode.getLAndExpNode());

            // If left was true, evaluate right side in thenBlock
            curBlock = thenBlock;
            curTrueBlock = trueBlock;
            curFalseBlock = falseBlock;
            tmpValue = null;
            visitEqExp(lAndExpNode.getEqExpNode());
            buildFactory.buildBr(curBlock, tmpValue, curTrueBlock, curFalseBlock);
        } else {
            // Leaf node: just an EqExp
            tmpValue = null;
            visitEqExp(lAndExpNode.getEqExpNode());
            buildFactory.buildBr(curBlock, tmpValue, curTrueBlock, curFalseBlock);
        }
    }

    private void visitLOrExp(LOrExpNode lOrExpNode) {
        // LOrExp → LAndExp | LOrExp '||' LAndExp
        // Note: Parser builds this as LOrExpNode(rightLAndExp, op, leftLOrExp)
        // So we need to process LEFT (getLOrExpNode) first, then RIGHT (getLAndExpNode)
        BasicBlock trueBlock = curTrueBlock;
        BasicBlock falseBlock = curFalseBlock;

        if (lOrExpNode.getLOrExpNode() != null) {
            // Has left side: first evaluate left LOrExp
            BasicBlock thenBlock = buildFactory.buildBasicBlock(curFunction);
            curTrueBlock = trueBlock;
            curFalseBlock = thenBlock;

            // Visit left side first
            visitLOrExp(lOrExpNode.getLOrExpNode());

            // If left was false, evaluate right side in thenBlock
            curBlock = thenBlock;
            curTrueBlock = trueBlock;
            curFalseBlock = falseBlock;
            visitLAndExp(lOrExpNode.getLAndExpNode());
        } else {
            // Leaf node: just a LAndExp
            visitLAndExp(lOrExpNode.getLAndExpNode());
        }
    }

    private void visitConstExp(ConstExpNode constExpNode) {
        // ConstExp -> AddExp
        isConst = true;
        saveValue = null;
        visitAddExp(constExpNode.getAddExpNode());
        isConst = false;
    }

    private void visitForStmt(ForStmtNode forStmtNode) {
        // ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
        for (int i = 0; i < forStmtNode.getlValNode().size(); i++) {
            LValNode lValNode = forStmtNode.getlValNode().get(i);
            ExpNode expNode = forStmtNode.getExpNode().get(i);

            if (lValNode.getExpNodes().isEmpty()) {
                // 普通变量赋值
                Value input = getValue(lValNode.getIdent().getContent());
                visitExp(expNode);
                tmpValue = buildFactory.buildStore(curBlock, input, tmpValue);
            } else {
                // 一维数组元素赋值
                // 计算数组下标
                visitExp(lValNode.getExpNodes().get(0));
                Value index = tmpValue;

                // 获取数组地址inde
                tmpValue = getValue(lValNode.getIdent().getContent());
                Type targetType = ((PointerType) tmpValue.getType()).getTargetType();
                Value addr;

                if (targetType instanceof PointerType) {
                    // 函数形参数组: int arr[]
                    // 类型: i32** -> 需要先 load 出 i32*
                    tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                    addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(index));
                } else {
                    // 局部/全局数组: int arr[10]
                    // 类型: [10 x i32]* -> 需要添加首索引 0
                    addr = buildFactory.buildGEP(curBlock, tmpValue, Arrays.asList(ConstInt.ZERO, index));
                }

                // 计算右侧表达式并存储
                visitExp(expNode);
                tmpValue = buildFactory.buildStore(curBlock, addr, tmpValue);
            }
        }
    }
}
