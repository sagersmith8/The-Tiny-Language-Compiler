import java.io.*;
import java.util.*;

public class CodeGenerator {
    private AST.ASTNode root;
    private static final String MAIN_FUNCTION = "main";
    public final List<IRNode> irCode = new ArrayList<IRNode>();
    public CodeGenerator(AST.ASTNode root) {
	this.root = root;

	assert root.type == AST.Type.Program;

	AST.ASTNode declList = root.children.get(1);
	assert declList.type == AST.Type.DeclarationList;
	assert declList.scope != null;
	
	if(declList.scope.getDeclared().size() > 0)
	    irCode.add(new IRNode(declList.scope));
	
	AST.ASTNode funcList = root.children.get(2);
	assert funcList.type == AST.Type.FunctionList;

	AST.ASTNode mainFunction = null;
	for(AST.ASTNode child : funcList.children) {
	    assert child.type == AST.Type.Function;

	    String name = child.children.get(1).value;
	    if(name.equals(MAIN_FUNCTION)) {
		mainFunction = child;
		break;
	    }
	}
	if(mainFunction == null)
	    throw new CompileException("Could not find main method");

	AST.ASTNode mainStmts = mainFunction.children.get(4);
	assert mainStmts.type == AST.Type.StatementList;
	assert mainStmts.scope != null;

	if(mainStmts.scope.getDeclared().size() > 0)
	    irCode.add(new IRNode(mainStmts.scope));
	
	for(AST.ASTNode stmt : mainStmts.children) {
	    generateCode(stmt, mainStmts.scope);
	}
    }

    public List<IRNode> getIRCode() {
	return irCode;
    }

    private static final String INT = "INT";
    private static final String FLOAT = "FLOAT";
    private static final String STRING = "STRING";
    private void generateCode(AST.ASTNode stmt, Scope scope) {
	if(stmt.type == AST.Type.Assignment) {
	    assert stmt.children.size() == 2;
	    
	    AST.ASTNode left = stmt.children.get(0);
	    AST.ASTNode right = stmt.children.get(1);

	    assert left.type == AST.Type.Identifier;
	    
	    Variable lhs = scope.getVariable(left.value);
	    if(lhs == null)
		throw new CompileException("Variable not defined in this scope",
					   lhs.nameToken);

	    ExprInfo result = generateExpr(right, scope);
	    if(lhs.type.equals(INT))
		irCode.add(new IRNode(IRNode.Type.STOREI, result.out, lhs.makeName()));
	    else if(lhs.type.equals(FLOAT))
		irCode.add(new IRNode(IRNode.Type.STOREF, result.out, lhs.makeName()));
	    else
		assert false;
	} else if(stmt.type == AST.Type.Read) {
	    for(AST.ASTNode operand : stmt.children) {
		assert operand.type == AST.Type.Identifier;

		Variable opVar = scope.getVariable(operand.value);

		if(opVar.type.equals(INT))
		    irCode.add(new IRNode(IRNode.Type.READI, opVar.makeName()));
		else if(opVar.type.equals(FLOAT))
		    irCode.add(new IRNode(IRNode.Type.READF, opVar.makeName()));
		else
		    assert false;
	    }
	} else if(stmt.type == AST.Type.Write) {
	    for(AST.ASTNode operand : stmt.children) {
		assert operand.type == AST.Type.Identifier;

		Variable opVar = scope.getVariable(operand.value);

		if(opVar.type.equals(INT))
		    irCode.add(new IRNode(IRNode.Type.WRITEI, opVar.makeName()));
		else if(opVar.type.equals(FLOAT))
		    irCode.add(new IRNode(IRNode.Type.WRITEF, opVar.makeName()));
		else if(opVar.type.equals(STRING))
		    irCode.add(new IRNode(IRNode.Type.WRITES, opVar.makeName()));
		else
		    assert false;
	    }
	} else if(stmt.type == AST.Type.If) {
	    boolean hasElse = stmt.children.size() == 5;
	    if(hasElse) {
		String elseLabel = newLabel();
		String exitLabel = newLabel();

		AST.ASTNode condition = stmt.children.get(0);
		assert condition.type == AST.Type.Condition;
		
		generateCondition(condition, scope, elseLabel);
		
		AST.ASTNode ifBlock = stmt.children.get(2);
		assert ifBlock.type == AST.Type.StatementList;
		assert ifBlock.scope != null;

		if(ifBlock.scope.getDeclared().size() > 0)
		    irCode.add(new IRNode(ifBlock.scope));
		
		for(AST.ASTNode substmt : ifBlock.children) {
		    generateCode(substmt, ifBlock.scope);
		}

		irCode.add(new IRNode(IRNode.Type.JUMP, exitLabel));
		irCode.add(new IRNode(IRNode.Type.LABEL, elseLabel));

		AST.ASTNode elseBlock = stmt.children.get(4);
		assert elseBlock.type == AST.Type.StatementList;
		assert elseBlock.scope != null;

		if(elseBlock.scope.getDeclared().size() > 0)
		    irCode.add(new IRNode(elseBlock.scope));
		
		for(AST.ASTNode substmt : elseBlock.children) {
		    generateCode(substmt, elseBlock.scope);
		}
		irCode.add(new IRNode(IRNode.Type.LABEL, exitLabel));
	    } else {
		String exitLabel = newLabel();

		AST.ASTNode condition = stmt.children.get(0);
		assert condition.type == AST.Type.Condition;
		
		generateCondition(condition, scope, exitLabel);
		
		AST.ASTNode ifBlock = stmt.children.get(2);
		assert ifBlock.type == AST.Type.StatementList;
		assert ifBlock.scope != null;

		if(ifBlock.scope.getDeclared().size() > 0)
		    irCode.add(new IRNode(ifBlock.scope));
		
		for(AST.ASTNode substmt : ifBlock.children) {
		    generateCode(substmt, ifBlock.scope);
		}
		irCode.add(new IRNode(IRNode.Type.LABEL, exitLabel));
	    }
	} else if(stmt.type == AST.Type.While) {
	    String loopLabel = newLabel();
	    String exitLabel = newLabel();

	    irCode.add(new IRNode(IRNode.Type.LABEL, loopLabel));
	    
	    AST.ASTNode condition = stmt.children.get(0);
	    assert condition.type == AST.Type.Condition;

	    generateCondition(condition, scope, exitLabel);
		
	    AST.ASTNode whileBlock = stmt.children.get(2);
	    assert whileBlock.type == AST.Type.StatementList;
	    assert whileBlock.scope != null;

	    if(whileBlock.scope.getDeclared().size() > 0)
		irCode.add(new IRNode(whileBlock.scope));
	    
	    for(AST.ASTNode substmt : whileBlock.children) {
		generateCode(substmt, whileBlock.scope);
	    }
	    
	    irCode.add(new IRNode(IRNode.Type.JUMP, loopLabel));
	    irCode.add(new IRNode(IRNode.Type.LABEL, exitLabel));
	} else
	    assert false;
    }

    static class ExprInfo {
	public String out;
	public String type;
    }

    private void generateCondition(AST.ASTNode cond, Scope scope, String exitLabel) {
	assert cond.children.size() == 1;

	AST.ASTNode condition = cond.children.get(0);
	assert condition.children.size() == 2;
	
	ExprInfo left = generateExpr(condition.children.get(0), scope);
	ExprInfo right = generateExpr(condition.children.get(1), scope);

	switch(condition.type) {
	case LessThan:
	    generateCondition(IRNode.Type.GEI, IRNode.Type.GEF, left, right, exitLabel);
	    break;
	case LessThanEqual:
	    generateCondition(IRNode.Type.GTI, IRNode.Type.GTF, left, right, exitLabel);
	    break;
	case GreaterThan:
	    generateCondition(IRNode.Type.LEI, IRNode.Type.LEF, left, right, exitLabel);
	    break;
	case GreaterThanEqual:
	    generateCondition(IRNode.Type.LTI, IRNode.Type.LTF, left, right, exitLabel);
	    break;
	case Equal:
	    generateCondition(IRNode.Type.NEI, IRNode.Type.NEF, left, right, exitLabel);
	    break;
	case NotEqual:
	    generateCondition(IRNode.Type.EQI, IRNode.Type.EQF, left, right, exitLabel);
	    break;
	}
    }

    private void generateCondition(IRNode.Type branchi, IRNode.Type branchf, ExprInfo left, ExprInfo right, String exitLabel) {
	if(left.type.equals(FLOAT) || right.type.equals(FLOAT)) {
	    irCode.add(new IRNode(branchf, left.out, right.out, exitLabel));
	} else if(left.type.equals(INT) && right.type.equals(INT)) {
	    irCode.add(new IRNode(branchi, left.out, right.out, exitLabel));
	} else
	    assert false;
    }
    
    private ExprInfo generateExpr(AST.ASTNode expr, Scope scope) {
	ExprInfo info = new ExprInfo();
	info.out = newRegister();
	if(expr.type == AST.Type.IntLiteral) {
	    irCode.add(new IRNode(IRNode.Type.STOREI, expr.value, info.out));
	    info.type = INT;
	} else if(expr.type == AST.Type.FloatLiteral) {
	    irCode.add(new IRNode(IRNode.Type.STOREF, expr.value, info.out));
	    info.type = FLOAT;
	} else if(expr.type == AST.Type.Identifier) {
	    Variable var = scope.getVariable(expr.value);
	    if(var.type.equals(INT)) {
		irCode.add(new IRNode(IRNode.Type.STOREI, var.makeName(), info.out));
		info.type = INT;
	    } else if(var.type.equals(FLOAT)) {
		irCode.add(new IRNode(IRNode.Type.STOREF, var.makeName(), info.out));
		info.type = FLOAT;
	    } else
		assert false;
	} else if(expr.type == AST.Type.Addition) {
	    generateBinaryExpr(expr, info, scope, IRNode.Type.ADDI, IRNode.Type.ADDF);
	} else if(expr.type == AST.Type.Subtraction) {
	    generateBinaryExpr(expr, info, scope, IRNode.Type.SUBI, IRNode.Type.SUBF);
	} else if(expr.type == AST.Type.Multiplication) {
	    generateBinaryExpr(expr, info, scope, IRNode.Type.MULTI, IRNode.Type.MULTF);	    
	} else if(expr.type == AST.Type.Division) {
	    generateBinaryExpr(expr, info, scope, IRNode.Type.DIVI, IRNode.Type.DIVF);	    
	} else
	    assert false;
	    
	return info;
    }

    private void generateBinaryExpr(AST.ASTNode expr, ExprInfo info, Scope scope, IRNode.Type typei, IRNode.Type typef) {
	ExprInfo argInfo1 = generateExpr(expr.children.get(0), scope);
	ExprInfo argInfo2 = generateExpr(expr.children.get(1), scope);
	    
	if(argInfo1.type.equals(FLOAT) || argInfo2.type.equals(FLOAT)) {
	    irCode.add(new IRNode(typef, argInfo1.out, argInfo2.out, info.out));
	    info.type = FLOAT;
	} else if(argInfo1.type.equals(INT) && argInfo2.type.equals(INT)) {
	    irCode.add(new IRNode(typei, argInfo1.out, argInfo2.out, info.out));
	    info.type = INT;
	} else
	    assert false;
    }

    private int regCounter = 0;
    private String newRegister() {
	return "$T"+(regCounter++);
    }

    private int labelCounter = 0;
    private String newLabel() {
	return "label"+(labelCounter++);
    }

    public static void main(String[] args) throws IOException {
	String input = new String(java.nio.file.Files.readAllBytes(new java.io.File(args[0]).toPath()));
	Token[] toks = new LittleScanner(input).getTokens();
	try {
	    LittleParser parser = new LittleParser(toks);

	    CodeGenerator gen = new CodeGenerator(parser.getAST().getRoot());
	    StringBuffer irBuf = new StringBuffer();
	    StringBuffer tinyBuf = new StringBuffer();
	    for(IRNode node : gen.getIRCode()) {
		String irOut = node.toString();
		if(irOut != null) {
		    irBuf.append(";");
		    irBuf.append(node.toString());
		    irBuf.append("\n");
		}
		tinyBuf.append(node.translate());
	    }
	    System.out.println(irBuf);
	    System.out.println(tinyBuf);
	} catch(CompileException c) {
	    ErrorPrinter.printError(args[0], c);
	}
    }
}
