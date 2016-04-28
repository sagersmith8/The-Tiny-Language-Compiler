public class Token {
    public final String value;
    public final Type type;
    public final int line;
    public final int col;
    
    public Token(Type type, String value) {
	this.value = value;
	this.type = type;
	this.line = -1;
	this.col = -1;
    }

    public Token(Type type, String value, int line, int col) {
	this.value = value;
	this.type = type;
	this.line = line;
	this.col = col-value.length();
    }
    
    enum Type {
	STRINGLITERAL("STRINGLITERAL"),
	INTLITERAL("INTLITERAL"),
	FLOATLITERAL("FLOATLITERAL"),
	OPERATOR("OPERATOR"),
	IDENTIFIER("IDENTIFIER"),
	KEYWORD("KEYWORD");

	public final String name;
	
	private Type(String name) {
	    this.name = name;
	}
    }
}
