import java.util.*;

public class LittleParser {
    private int pos = 0;
    private Token[] tokens;

    private int blockNumber = 1;
    private Stack<Scope> scopeStack = new Stack<Scope>();
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

	require(PROGRAM);
	require(Token.Type.IDENTIFIER);
	require(BEGIN);
	parsePgmBody();
	require(END);

	closeScope();
    }

    private void parsePgmBody() {
	parseDecls();
	parseFuncDecls();
    }

    private boolean matchDecl() {
	return match(STRING, true) ||
	    match(FLOAT, true) ||
	    match(INT, true);
    }

    private void parseDecls() {
	while(matchDecl()) {
	    parseDecl();
	}
    }
    
    private void parseDecl() {
	if(matchStringDecl()) {
	    parseStringDecl();
	}else if(matchVarDecl()) {
	    parseVarDecl();
	}
    }

    private boolean matchStringDecl() {
	return match(STRING, true);
    }
    
    private void parseStringDecl() {
	require(STRING);
	require(Token.Type.IDENTIFIER);
	require(ASSIGN_OPERATOR);
	require(Token.Type.STRINGLITERAL);
	require(STMT_END);

	currentScope().addVariable(new Variable(tokens[pos-4], tokens[pos-5].value, tokens[pos-2].value));
    }

    private boolean matchVarDecl() {
	return match(FLOAT, true) ||
	    match(INT, true);
    }

    private void parseVarDecl() {
	String varType = null;
	List<Token> varNames = null;

	varType = parseVarTypeAsString();
	varNames = parseIdListAsList();
	require(STMT_END);

	for(Token varName : varNames) {
	    currentScope().addVariable(new Variable(varName, varType));
	}
    }

    private boolean matchVarType() {
	return match(INT, true) ||
	    match(VOID, true);
    }

    
    private void parseVarType() {
	if(!(match(FLOAT) || match(INT)))
	    throw new CompileException("Expected Variable Type", tokens[pos]);
    }

    private String parseVarTypeAsString() {
	if(match(FLOAT, false, true))
	    return FLOAT.value;
	if(match(INT, false, true))
	    return INT.value;
	throw new CompileException("Expected variable type", tokens[pos]);
    }

    private void parseIdList() {
	do {
	    require(Token.Type.IDENTIFIER);
	} while(match(COMMA_SEPARATOR));
    }

    private List<Token> parseIdListAsList() {
	List<Token> ids = new ArrayList<Token>();

	do {
	    require(Token.Type.IDENTIFIER);
	    ids.add(tokens[pos-1]);
	} while(match(COMMA_SEPARATOR));
	
	return ids;
    }

    private boolean matchFuncDecl() {
	return match(FUNCTION, true);
    }

    private void parseFuncDecls() {
	while(matchFuncDecl()) {
	    parseFuncDecl();
	}
    }

    private void parseFuncDecl() {
	require(FUNCTION);
	parseAnyType();
	require(Token.Type.IDENTIFIER);

	addNewScope(tokens[pos-1].value);

	require(LEFT_PAREN);
	parseParamList();
	require(RIGHT_PAREN);
	require(BEGIN);
	parseFuncBody();
	require(END);

	closeScope();

    }
    
    private void parseAnyType() {
	if(!(match(FLOAT) || match(INT) || match(VOID)))
	    throw new CompileException("Expected return type", tokens[pos]);
    }

    private void parseParamList() {
	if(matchParamDecl()) {
	    do {
		parseParamDecl();
	    } while(match(COMMA_SEPARATOR));
	}
    }

    private boolean matchParamDecl() {
	return matchVarType();
    }

    private void parseParamDecl() {
	parseVarType();
	require(Token.Type.IDENTIFIER);

	currentScope().addVariable(new Variable(tokens[pos-1], tokens[pos-2].value));
    }

    private void parseFuncBody() {
	parseDecls();
	parseStmtList();
    }

    private void parseStmtList() {
	while(matchStmt()) {
	    parseStmt();
	}
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
	return match(IF, true);
    }

    private boolean matchWhileStmt() {
	return match(WHILE, true);
    }

    private boolean matchAssignStmt() {
	return match(Token.Type.IDENTIFIER, true);
    }

    private boolean matchReadStmt() {
	return match(READ, true);
    }

    private boolean matchWriteStmt() {
	return match(WRITE, true);
    }

    private boolean matchReturnStmt() {
	return match(RETURN, true);
    }
    
    private void parseIfStmt() {
	addNewBlockScope();
	require(IF);
	require(LEFT_PAREN);
	parseCondition();
	require(RIGHT_PAREN);
	parseDecls();
	parseStmtList();
	parseElse();
	require(ENDIF);
    }

    private void parseWhileStmt() {
	addNewBlockScope();

	require(WHILE);
	require(LEFT_PAREN);
	parseCondition();
	require(RIGHT_PAREN);
	parseDecls();
	parseStmtList();
	require(ENDWHILE);

	closeScope();
    }

    private void parseReadStmt() {
	require(READ);
	require(LEFT_PAREN);
	parseIdList();
	require(RIGHT_PAREN);
	require(STMT_END);
    }

    private void parseWriteStmt() {
	require(WRITE);
	require(LEFT_PAREN);
	parseIdList();
	require(RIGHT_PAREN);
	require(STMT_END);
    }

    private void parseReturnStmt() {
	require(RETURN);
	parseExpr();
	require(STMT_END);
    }

    private void parseAssignStmt() {
	require(Token.Type.IDENTIFIER);
	require(ASSIGN_OPERATOR);
	parseExpr();
	require(STMT_END);
    }
    
    private void parseCondition() {
	parseExpr();
	parseCompOp();
	parseExpr();
    }

    private boolean parseCompOp() {
	return match(LESS_THAN) ||
	    match(GREATER_THAN) ||
	    match(LESS_THAN_EQUAL) ||
	    match(GREATER_THAN_EQUAL) ||
	    match(NOT_EQUAL) ||
	    match(EQUAL);
    }

    private void parseElse() {
	closeScope();
	if(match(ELSE)) {
	    addNewBlockScope();
	    
	    parseDecls();
	    parseStmtList();
	    
	    closeScope();
	}
    }

    private void parseExpr() {
	do {
	    parseFactor();
	} while(parseAddOp());
    }
    
    private boolean parseAddOp() {
	return match(ADD_OPERATOR) ||
	    match(SUBTRACT_OPERATOR);
    }

    private void parseFactor() {
	do {
	    parsePostfixExpr();
	} while(parseMulOp());
    }

    private boolean parseMulOp() {
	return match(MULTIPLY_OPERATOR) ||
	    match(DIVIDE_OPERATOR);
    }

    private boolean matchPostfixExpr() {
	return matchCallExpr() ||
	    match(LEFT_PAREN, true) ||
	    match(Token.Type.IDENTIFIER, true) ||
	    match(Token.Type.INTLITERAL, true) ||
	    match(Token.Type.FLOATLITERAL, true);
    }

    private void parsePostfixExpr() {
	if(matchCallExpr())
	    parseCallExpr();
	else if(match(LEFT_PAREN, true))
	    parseParenExpr();
	else if(!(match(Token.Type.IDENTIFIER) ||
		  match(Token.Type.INTLITERAL) ||
		  match(Token.Type.FLOATLITERAL)))
	    throw new CompileException("Illegal start of expression", tokens[pos]);
    }

    private void parseParenExpr() {
	require(LEFT_PAREN);
	parseExpr();
	require(RIGHT_PAREN);
    }

    private boolean matchCallExpr() {
	int startPos = pos;
	if(!match(Token.Type.IDENTIFIER)) {
	    pos = startPos;
	    return false;
	}
	if(!match(LEFT_PAREN)) {
	    pos = startPos;
	    return false;
	}
	pos = startPos;
	return true;
    }

    private void parseCallExpr() {
	require(Token.Type.IDENTIFIER);
	require(LEFT_PAREN);
	parseExprList();
	require(RIGHT_PAREN);
    }

    private void parseExprList() {
	if(matchExpr()) {
	    do {
		parseExpr();
	    } while(match(COMMA_SEPARATOR));
	}
    }

    private boolean matchExpr() {
	return matchPostfixExpr();
    }

    private void require(Token t) {
	require(t, false);
    }
    
    private void require(Token t, boolean stay) {
	if(pos >= tokens.length)
	    throw new CompileException("Reached end of file while parsing",
				       tokens[tokens.length-1]);
	Token cur = tokens[pos];

	if(t.type == cur.type && t.value.equals(cur.value)) {
	    if(!stay)
		pos++;

	    return;
	}
	throw new CompileException("Expected token '"+t.value+"' of type '"+t.type+"'",
				   tokens[pos]);
    }


    private void require(Token.Type t) {
	require(t, false);
    }

    private void require(Token.Type type, boolean stay) {
	if(pos >= tokens.length)
	    throw new CompileException("Reached end of file while parsing",
				       tokens[tokens.length-1]);
	Token cur = tokens[pos];
	
	if(type == cur.type) {
	    if(!stay)
		pos++;
	    return;
	}
	throw new CompileException("Expected token of type '"+type+"'",
				   tokens[pos]);

    }
    
    private boolean match(Token t) {
	return match(t, false, false);
    }

    private boolean match(Token t, boolean stay) {
	return match(t, stay, false);
    }
    
    
    private boolean match(Token t, boolean stay, boolean barfAtEnd) {
	if(pos >= tokens.length) {
	    if(barfAtEnd)
		throw new CompileException("Reached end of file while parsing",
					   tokens[tokens.length-1]);
	    return false;
	}
	Token cur = tokens[pos];

	if(t.type == cur.type && t.value.equals(cur.value)) {
	    if(!stay)
		pos++;

	    return true;
	}
	return false;
    }
    
    private boolean match(Token.Type t) {
	return match(t, false, false);
    }

    private boolean match(Token.Type t, boolean stay) {
	return match(t, stay, false);
    }

    private boolean match(Token.Type type, boolean stay, boolean barfAtEnd) {
	if(pos >= tokens.length) {
	    if(barfAtEnd)
		throw new CompileException("Reached end of file while parsing",
					   tokens[tokens.length-1]);
	    return false;
	}
	Token cur = tokens[pos];
	
	if(type == cur.type) {
	    if(!stay)
		pos++;
	    return true;
	}
	return false;
    }

    public static Token keyword(String val) {
	return new Token(Token.Type.KEYWORD, val);
    }

    public static Token operator(String val) {
	return new Token(Token.Type.OPERATOR, val);
    }
    
    public static void main(String[] args) throws java.io.IOException {
	String input = new String(java.nio.file.Files.readAllBytes(new java.io.File(args[0]).toPath()));
	Token[] toks = new LittleScanner(input).getTokens();
	try {
	    LittleParser parser = new LittleParser(toks);
	
	    Iterator<Scope> iter = parser.scopeList.iterator();
	    while(iter.hasNext()) {
		System.out.print(iter.next());
		if(iter.hasNext()) {
		    System.out.println();
		    System.out.println();
		}
	    }
	} catch(CompileException c) {
	    List<String> lines = java.nio.file.Files.readAllLines(new java.io.File(args[0]).toPath(),
								  java.nio.charset.Charset.forName("UTF-8"));

	    String line = lines.get(c.line-1);
	    int col = c.col;
	    String shortLine = line.replaceAll("\t","");
	    int tabs = line.length() - shortLine.length();
	    col += 5*tabs;

	    System.out.println("Line "+c.line+": "+c.msg);
	    System.out.println(lines.get(c.line-1));
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
