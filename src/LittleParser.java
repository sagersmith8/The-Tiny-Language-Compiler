import java.util.*;

public class LittleParser {
    private int pos = 0;
    private Token[] tokens;

    private int blockNumber = 1;
    private Stack<Scope> scopeStack = new Stack<Scope>();
    private AST ast = new AST();
    private List<Scope> scopeList = new LinkedList<Scope>();
	
    private static final Token PROGRAM = keyword("PROGRAM");
    private static final Token BEGIN = keyword("BEGIN");
    private static final Token END = keyword("END");

    private static final Token FUNCTION = keyword("FUNCTION");

    private static final Token IF = keyword("IF");
    private static final Token ELSE = keyword("ELSE");
    private static final Token ENDIF = keyword("ENDIF");
    private static final Token WHILE = keyword("WHILE");
    private static final Token ENDWHILE = keyword("ENDWHILE");

    private static final Token READ = keyword("READ");
    private static final Token WRITE = keyword("WRITE");
    private static final Token RETURN = keyword("RETURN");
    
    private static final Token STRING = keyword("STRING");
    private static final Token FLOAT = keyword("FLOAT");
    private static final Token INT = keyword("INT");
    private static final Token VOID = keyword("VOID");

    private static final Token LEFT_PAREN = operator("(");
    private static final Token RIGHT_PAREN = operator(")");
    
    private static final Token ASSIGN_OPERATOR = operator(":=");
    private static final Token COMMA_SEPARATOR = operator(",");
    private static final Token STMT_END = operator(";");

    private static final Token GREATER_THAN = operator(">");
    private static final Token LESS_THAN = operator("<");
    private static final Token GREATER_THAN_EQUAL = operator(">=");
    private static final Token LESS_THAN_EQUAL = operator("<=");
    private static final Token EQUAL = operator("=");
    private static final Token NOT_EQUAL = operator("!=");

    private static final Token ADD_OPERATOR = operator("+");
    private static final Token SUBTRACT_OPERATOR = operator("-");
    private static final Token MULTIPLY_OPERATOR = operator("*");
    private static final Token DIVIDE_OPERATOR = operator("/");
    
    public LittleParser(Token[] tokens) {
	this.tokens = tokens;
	parseProgram();
    }

    private void addNewBlockScope() {
	addNewScope("BLOCK "+blockNumber);
	blockNumber++;
    }

    private void addNewScope(String name) {
	scopeStack.push(new Scope(name));
	scopeList.add(scopeStack.peek());
    }

    private Scope currentScope() {
	return scopeStack.peek();
    }

    private void closeScope() {
	scopeStack.pop();
    }

    private void parseProgram() {
	addNewScope("GLOBAL");

	requireToken(PROGRAM);
	requireTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value));
	requireToken(BEGIN);
	parsePgmBody();
	requireToken(END);

	closeScope();

	ast.buildNode(AST.Type.Program, 3);
    }

    private void parsePgmBody() {
	parseDecls();
	parseFuncDecls();
    }

    private boolean matchDecl() {
	return seeToken(STRING) ||
	    seeToken(FLOAT) ||
	    seeToken(INT);
    }

    private void parseDecls() {
	ast.startMark();
	while(matchDecl()) {
	    parseDecl();
	}
	ast.buildNode(AST.Type.DeclarationList, ast.endMark());
    }
    
    private void parseDecl() {
	if(matchStringDecl()) {
	    parseStringDecl();
	}else if(matchVarDecl()) {
	    parseVarDecl();
	}
    }

    private boolean matchStringDecl() {
	return seeToken(STRING);
    }
    
    private void parseStringDecl() {
	requireToken(STRING);
	requireTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value));
	requireToken(ASSIGN_OPERATOR);
	requireTypeBuild(Token.Type.STRINGLITERAL, (Token t) -> ast.buildNode(AST.Type.StringLiteral, t.value));
	requireToken(STMT_END);

	currentScope().addVariable(new Variable(tokens[pos-4], tokens[pos-5].value, tokens[pos-2].value));

	ast.buildNode(AST.Type.StringDeclaration, 2);
    }

    private boolean matchVarDecl() {
	return seeToken(FLOAT) ||
	    seeToken(INT);
    }

    private void parseVarDecl() {
	AST.Type varType = null;
	List<Token> varNames = null;

	varType = parseVarTypeAsType();
	varNames = parseIdListAsList();
	requireToken(STMT_END);

	ast.startMark();
	ast.buildNode(varType);
	for(Token varName : varNames) {
	    currentScope().addVariable(new Variable(varName, (varType == AST.Type.IntType ? "INT" : "FLOAT")));
	    ast.buildNode(AST.Type.Identifier, varName.value);
	}
	ast.buildNode(AST.Type.VariableDeclaration, ast.endMark());
    }

    private boolean matchVarType() {
	return seeToken(INT) ||
	    seeToken(FLOAT);
    }

    
    private void parseVarType() {
	if(!(matchTokenBuild(FLOAT, (Token t) -> ast.buildNode(AST.Type.FloatType)) ||
	     matchTokenBuild(INT, (Token t) -> ast.buildNode(AST.Type.IntType))))
	    throw new CompileException("Expected Variable Type", tokens[pos]);
    }

    private AST.Type parseVarTypeAsType() {
	if(matchToken(FLOAT))
	    return AST.Type.FloatType;
	if(matchToken(INT))
	    return AST.Type.IntType;
	throw new CompileException("Expected variable type", tokens[pos]);
    }

    private void parseIdList() {
	do {
	    requireType(Token.Type.IDENTIFIER);
	} while(matchToken(COMMA_SEPARATOR));
    }

    private List<Token> parseIdListAsList() {
	List<Token> ids = new ArrayList<Token>();

	do {
	    requireType(Token.Type.IDENTIFIER);
	    ids.add(tokens[pos-1]);
	} while(matchToken(COMMA_SEPARATOR));
	
	return ids;
    }

    private boolean matchFuncDecl() {
	return seeToken(FUNCTION);
    }

    private void parseFuncDecls() {
	ast.startMark();
	while(matchFuncDecl()) {
	    parseFuncDecl();
	}
	ast.buildNode(AST.Type.FunctionList, ast.endMark());
    }

    private void parseFuncDecl() {
	requireToken(FUNCTION);
	parseAnyType();
	requireTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value));

	addNewScope(tokens[pos-1].value);

	requireToken(LEFT_PAREN);
	parseParamList();
	requireToken(RIGHT_PAREN);
	requireToken(BEGIN);
	parseFuncBody();
	requireToken(END);

	closeScope();

	ast.buildNode(AST.Type.Function, 5);
    }
    
    private void parseAnyType() {
	if(!(matchTokenBuild(FLOAT, (Token t) -> ast.buildNode(AST.Type.FloatType)) ||
	     matchTokenBuild(INT, (Token t) -> ast.buildNode(AST.Type.IntType)) ||
	     matchTokenBuild(VOID, (Token t) -> ast.buildNode(AST.Type.VoidType))))
	    throw new CompileException("Expected return type", tokens[pos]);
    }

    private void parseParamList() {
	ast.startMark();
	if(matchParamDecl()) {
	    do {
		parseParamDecl();
	    } while(matchToken(COMMA_SEPARATOR));
	}
	ast.buildNode(AST.Type.ParameterList, ast.endMark());
    }

    private boolean matchParamDecl() {
	return matchVarType();
    }

    private void parseParamDecl() {
	parseVarType();
	requireTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value));

	ast.buildNode(AST.Type.Parameter, 2);
	currentScope().addVariable(new Variable(tokens[pos-1], tokens[pos-2].value));
    }

    private void parseFuncBody() {
	parseDecls();
	parseStmtList();
    }

    private void parseStmtList() {
	ast.startMark();
	while(matchStmt()) {
	    parseStmt();
	}
	ast.buildNode(AST.Type.StatementList, ast.endMark());
    }

    private boolean matchStmt() {
	return matchIfStmt() ||
	    matchWhileStmt() ||
	    matchAssignStmt() ||
	    matchReadStmt() ||
	    matchWriteStmt() ||
	    matchReturnStmt();
    }

    private void parseStmt() {
	if(matchIfStmt())
	    parseIfStmt();
	else if(matchWhileStmt())
	    parseWhileStmt();
	else if(matchAssignStmt())
	    parseAssignStmt();
	else if(matchReadStmt())
	    parseReadStmt();
	else if(matchWriteStmt())
	    parseWriteStmt();
	else if(matchReturnStmt())
	    parseReturnStmt();
	else
	    throw new CompileException("THIS SHOULDN'T HAPPEN", tokens[pos]);
    }

    private boolean matchIfStmt() {
	return seeToken(IF);
    }

    private boolean matchWhileStmt() {
	return seeToken(WHILE);
    }

    private boolean matchAssignStmt() {
	return seeType(Token.Type.IDENTIFIER);
    }

    private boolean matchReadStmt() {
	return seeToken(READ);
    }

    private boolean matchWriteStmt() {
	return seeToken(WRITE);
    }

    private boolean matchReturnStmt() {
	return seeToken(RETURN);
    }
    
    private void parseIfStmt() {
	ast.startMark();
	addNewBlockScope();
	requireToken(IF);
	requireToken(LEFT_PAREN);
	parseCondition();
	requireToken(RIGHT_PAREN);
	parseDecls();
	parseStmtList();
	parseElse();
	requireToken(ENDIF);

	ast.buildNode(AST.Type.If, ast.endMark());
    }

    private void parseWhileStmt() {
	addNewBlockScope();

	requireToken(WHILE);
	requireToken(LEFT_PAREN);
	parseCondition();
	requireToken(RIGHT_PAREN);
	parseDecls();
	parseStmtList();
	requireToken(ENDWHILE);

	ast.buildNode(AST.Type.While, 3);
	
	closeScope();
    }

    private void parseReadStmt() {
	requireToken(READ);
	requireToken(LEFT_PAREN);
	List<Token> tokens = parseIdListAsList();
	requireToken(RIGHT_PAREN);
	requireToken(STMT_END);

	for(Token t : tokens ) {
	    ast.buildNode(AST.Type.Identifier, t.value);
	}
	ast.buildNode(AST.Type.Read, tokens.size());
    }

    private void parseWriteStmt() {
	requireToken(WRITE);
	requireToken(LEFT_PAREN);
	List<Token> tokens = parseIdListAsList();
	requireToken(RIGHT_PAREN);
	requireToken(STMT_END);

	for(Token t : tokens ) {
	    ast.buildNode(AST.Type.Identifier, t.value);
	}
	ast.buildNode(AST.Type.Write, tokens.size());
    }

    private void parseReturnStmt() {
	requireToken(RETURN);
	parseExpr();
	requireToken(STMT_END);
    }

    private void parseAssignStmt() {
	requireTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value));
	requireToken(ASSIGN_OPERATOR);
	parseExpr();
	requireToken(STMT_END);

	ast.buildNode(AST.Type.Assignment, 2);
    }
    
    private void parseCondition() {
	parseExpr();
	AST.Type compType = parseCompOp();
	parseExpr();
	ast.buildNode(compType, 2);
	ast.buildNode(AST.Type.Condition, 1);
    }

    private AST.Type parseCompOp() {
	if(matchToken(LESS_THAN)) return AST.Type.LessThan;
	if(matchToken(GREATER_THAN)) return AST.Type.GreaterThan;
	if(matchToken(LESS_THAN_EQUAL)) return AST.Type.LessThanEqual;
	if(matchToken(GREATER_THAN_EQUAL)) return AST.Type.GreaterThanEqual;
	if(matchToken(NOT_EQUAL)) return AST.Type.NotEqual;
	if(matchToken(EQUAL)) return AST.Type.Equal;
	return null;
    }

    private void parseElse() {
	closeScope();
	if(matchToken(ELSE)) {
	    addNewBlockScope();
	    
	    parseDecls();
	    parseStmtList();
	    
	    closeScope();
	}
    }

    private void parseExpr() {
	AST.Type addOp = null;
	do {
	    parseFactor();
	    if(addOp != null)
		ast.buildNode(addOp, 2);
	} while((addOp = parseAddOp()) != null);
    }
    
    private AST.Type parseAddOp() {
	if(matchToken(ADD_OPERATOR))
	    return AST.Type.Addition;
	if(matchToken(SUBTRACT_OPERATOR))
	    return AST.Type.Subtraction;
	return null;
    }

    private void parseFactor() {
	AST.Type mulOp = null;
	do {
	    parsePostfixExpr();
	    if(mulOp != null)
		ast.buildNode(mulOp, 2);
	} while((mulOp = parseMulOp()) != null);
    }

    private AST.Type parseMulOp() {
	if(matchToken(MULTIPLY_OPERATOR))
	    return AST.Type.Multiplication;
	if(matchToken(DIVIDE_OPERATOR))
	    return AST.Type.Division;
	return null;
    }

    private boolean matchPostfixExpr() {
	return matchCallExpr() ||
	    seeToken(LEFT_PAREN) ||
	    seeType(Token.Type.IDENTIFIER) ||
	    seeType(Token.Type.INTLITERAL) ||
	    seeType(Token.Type.FLOATLITERAL);
    }

    private void parsePostfixExpr() {
	if(matchCallExpr())
	    parseCallExpr();
	else if(seeToken(LEFT_PAREN))
	    parseParenExpr();
	else if(!(matchTypeBuild(Token.Type.IDENTIFIER, (Token t) -> ast.buildNode(AST.Type.Identifier, t.value)) ||
		  matchTypeBuild(Token.Type.INTLITERAL, (Token t) -> ast.buildNode(AST.Type.IntLiteral, t.value)) ||
		  matchTypeBuild(Token.Type.FLOATLITERAL, (Token t) -> ast.buildNode(AST.Type.FloatLiteral, t.value))))
	    throw new CompileException("Illegal start of expression", tokens[pos]);
    }

    private void parseParenExpr() {
	requireToken(LEFT_PAREN);
	parseExpr();
	requireToken(RIGHT_PAREN);
    }

    private boolean matchCallExpr() {
	int startPos = pos;
	if(!matchType(Token.Type.IDENTIFIER)) {
	    pos = startPos;
	    return false;
	}
	if(!matchToken(LEFT_PAREN)) {
	    pos = startPos;
	    return false;
	}
	pos = startPos;
	return true;
    }

    private void parseCallExpr() {
	requireType(Token.Type.IDENTIFIER);
	requireToken(LEFT_PAREN);
	parseExprList();
	requireToken(RIGHT_PAREN);
    }

    private void parseExprList() {
	if(matchExpr()) {
	    do {
		parseExpr();
	    } while(matchToken(COMMA_SEPARATOR));
	}
    }

    private boolean matchExpr() {
	return matchPostfixExpr();
    }

    private TokenMatcher<Token.Type> typeMatcher = new TokenMatcher<Token.Type>() {
	    private Token.Type type;
	    public boolean matches(Token t) {
		return t.type == type;
	    }
	    public String error() {
		return "Expected token of type '"+type+"'";
	    }
	    public void setup(Token.Type type) {
		this.type = type;
	    }
	};

    private TokenMatcher<Token> tokenMatcher = new TokenMatcher<Token>() {
	    private Token token;
	    public boolean matches(Token t) {
		return token.type == t.type && token.value.equals(t.value);
	    }
	    public String error() {
		return "Expected token: '"+token.value+"'";
	    }
	    public void setup(Token token) {
		this.token = token;
	    }
	};

    private interface TokenMatcher<T> {
	public boolean matches(Token t);
	public String error();
	public void setup(T conf);
    }

    private void requireType(Token.Type type) {
	typeMatcher.setup(type);
	match(typeMatcher, false, true);
    }

    private void requireToken(Token token) {
	tokenMatcher.setup(token);
	match(tokenMatcher, false, true);
    }

    private boolean matchType(Token.Type type) {
	typeMatcher.setup(type);
	return match(typeMatcher, false, false) != null;
    }

    private boolean matchToken(Token token) {
	tokenMatcher.setup(token);
	return match(tokenMatcher, false, false) != null;
    }

    private boolean seeType(Token.Type type) {
	typeMatcher.setup(type);
	return match(typeMatcher, true, false) != null;
    }

    private boolean seeToken(Token token) {
	tokenMatcher.setup(token);
	return match(tokenMatcher, true, false) != null;
    }

    private interface ASTNodeBuilder {
	public void build(Token src);
    }

    private boolean matchTypeBuild(Token.Type type, ASTNodeBuilder builder) {
	typeMatcher.setup(type);
	Token res = match(typeMatcher, false, false);
	if(res != null) {
	    builder.build(res);
	    return true;
	}
	return false;
    }

    private boolean matchTokenBuild(Token token, ASTNodeBuilder builder) {
	tokenMatcher.setup(token);
	Token res = match(tokenMatcher, false, false);
	if(res != null) {
	    builder.build(res);
	    return true;
	}
	return false;
    }

    private void requireTypeBuild(Token.Type type, ASTNodeBuilder builder) {
	typeMatcher.setup(type);
	Token res = match(typeMatcher, false, true);
	if(res != null) {
	    builder.build(res);
	}
    }

    private void requireTokenBuild(Token token, ASTNodeBuilder builder) {
	tokenMatcher.setup(token);
	Token res = match(tokenMatcher, false, true);
	if(res != null) {
	    builder.build(res);
	}
    }


    private Token match(TokenMatcher m, boolean stay, boolean required) {
	if(pos >= tokens.length) {
	    if(required) {
		throw new CompileException("Reached end of file while parsing",
					   tokens[tokens.length-1]);
	    }
	    return null;
	}

	Token cur = tokens[pos];
	if(m.matches(cur)) {
	    if(!stay)
		pos++;
	    return cur;
	}
	if(required) {
	    throw new CompileException(m.error(),
				       tokens[pos]);
	}
	return null;
    }

    public static Token keyword(String val) {
	return new Token(Token.Type.KEYWORD, val);
    }

    public static Token operator(String val) {
	return new Token(Token.Type.OPERATOR, val);
    }

    public AST getAST() {
	return ast;
    }
    
    public static void main(String[] args) throws java.io.IOException {
	String input = new String(java.nio.file.Files.readAllBytes(new java.io.File(args[0]).toPath()));
	Token[] toks = new LittleScanner(input).getTokens();
	try {
	    LittleParser parser = new LittleParser(toks);

	    parser.getAST().print();
	    /*Iterator<Scope> iter = parser.scopeList.iterator();
	    while(iter.hasNext()) {
		System.out.print(iter.next());
		if(iter.hasNext()) {
		    System.out.println();
		    System.out.println();
		}
	    }*/
	} catch(CompileException c) {
	    List<String> lines = java.nio.file.Files.readAllLines(new java.io.File(args[0]).toPath(),
								  java.nio.charset.Charset.forName("UTF-8"));

	    String line = lines.get(c.line-1).trim();
	    int col = c.col;

	    System.out.println("Line "+c.line+": "+c.msg);
	    System.out.println(line);
	    System.out.println(pointerStr(col));
	}
    }

    private static final char POINTER = '^';
    public static String pointerStr(int loc) {
	char[] pointerChars = new char[loc+1];
	for(int i = 0; i < loc; i++) {
	    pointerChars[i] = ' ';
	}
	pointerChars[pointerChars.length-1] = POINTER;

	return new String(pointerChars);
    }
}
