import java.util.*;

public class LittleScanner {
    private char[] input;
    private String inputStr;
    private int pos = 0;
    private Token[] tokens;

    private int newLinePos = 0;
    private int curLine = 1;
    
    public LittleScanner(String program) {
	input = program.toCharArray();
	inputStr = program;
	
	List<Token> tokenList = new ArrayList<Token>();

	while(pos < input.length) {
	    scanWhitespace();
	    
	    if(pos >= input.length)
		break;
		
	    if(onComment()) {
		skipLine();
	    } else if(onQuote()) {
		tokenList.add(new Token(Token.Type.STRINGLITERAL,
					scanStringLiteral(),
					curLine, pos - newLinePos));
	    } else if(onIdentifier()) {
		String ident = scanIdentifier();
		tokenList.add(new Token(isKeyword(ident) ? Token.Type.KEYWORD :	Token.Type.IDENTIFIER,
					ident,
					curLine, pos - newLinePos));
	    } else if(onNumber()) {
		tokenList.add(scanNumberToken());
	    } else {
		Token opToken = scanOperatorToken();
		
		if(opToken == null)
		    throw new CompileException("Unexpected Token",curLine, pos-newLinePos);

		tokenList.add(opToken);
	    }
	}
	tokens = new Token[tokenList.size()];
	tokenList.toArray(tokens);
    }

    private static final List<String> OPERATORS =
	Arrays.asList(":=","+","-","*","/","=","!=","<",">","(",")",";",",","<=",">=");
    private Token scanOperatorToken() {
	if(pos+1 < input.length) {
	    String duOp = inputStr.substring(pos,pos+2);
	    if(OPERATORS.contains(duOp)) {
		pos=pos+2;
		return new Token(Token.Type.OPERATOR, duOp, curLine, pos - newLinePos);
	    }
	}

	String unOp = inputStr.substring(pos,pos+1);
	if(OPERATORS.contains(unOp)) {
	    pos=pos+1;
	    return new Token(Token.Type.OPERATOR, unOp, curLine, pos - newLinePos);
	}
	
	return null;
    }

    private boolean onNumber() {
	return Character.isDigit(input[pos]) || input[pos] == '.';
    }

    private Token scanNumberToken() {
	int start = pos;
	boolean foundDot = input[pos] == '.';
	pos++;
	while(pos < input.length && Character.isDigit(input[pos]) || (!foundDot && input[pos] == '.')) {
	    if(input[pos] == '.')
		foundDot = true;
	    pos++;
	}
	if(foundDot && input[pos] == '.')
	    System.exit(1);
	return new Token(foundDot ? Token.Type.FLOATLITERAL : Token.Type.INTLITERAL,
			 inputStr.substring(start,pos),
			 curLine, pos - newLinePos);
    }
    
    private static final List<String> KEYWORDS =
	Arrays.asList("PROGRAM","BEGIN","END","FUNCTION","READ","WRITE","IF","ELSE","ENDIF","WHILE","ENDWHILE","CONTINUE","BREAK","RETURN","INT","VOID","STRING","FLOAT");
    public boolean isKeyword(String word) {
	return KEYWORDS.contains(word);
    }

    public boolean onIdentifier() {
	return Character.isLetter(input[pos]);
    }

    public String scanIdentifier() {
	int start = pos;
	while(pos < input.length && Character.isLetterOrDigit(input[pos])) {
	    pos++;
	}
	return inputStr.substring(start, pos);
    }

    public boolean onQuote() {
	return input[pos] == '"';
    }

    public String scanStringLiteral() {
	int start = ++pos;
	while(pos < input.length && !onQuote()) {
	    pos++;
	}
	pos++;
	return inputStr.substring(start, pos-1);
    }
    
    public static final char COMMENT_CHAR = '-';    
    public boolean onComment() {
	return pos+1 < input.length &&
	    input[pos] == COMMENT_CHAR &&
	    input[pos+1] == COMMENT_CHAR;
    }

    public void skipLine() {
	while(pos < input.length && !skipEOL()) {
	    pos++;
	}
    }

    public void scanWhitespace() {
	while(pos < input.length && Character.isWhitespace(input[pos])) {
	    if(!skipEOL())
		pos++;
	}
    }

    public boolean skipEOL() {
	if(input[pos] == '\n') {
	    pos++;

	    scanWhitespace();
	    newLinePos = pos;
	    curLine++;
	    return true;
	}
	if(input[pos] == '\r') {
	    pos++;
	    if(pos < input.length && input[pos] == '\n')
		pos++;

	    scanWhitespace();
	    newLinePos = pos;
	    curLine++;
	    return true;
	}
	return false;
    }

    public Token[] getTokens() {
	return tokens;
    }

    public static void main(String[] args) throws java.io.IOException {
	String input = new String(java.nio.file.Files.readAllBytes(new java.io.File(args[0]).toPath()));
	Token[] toks = new LittleScanner(input).getTokens();
	
	for(int i = 0; i < toks.length; i++) {
	    Token t = toks[i];

	    System.out.println("Token Type: "+t.type.name);
	    if(t.type == Token.Type.STRINGLITERAL)
		System.out.println("Value: \""+t.value+"\"");
	    else
		System.out.println("Value: "+t.value);
	}
    }
}
