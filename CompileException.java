public class CompileException extends RuntimeException {
    public final int line, col;
    public final String msg;
    
    public CompileException(String msg, int line, int col) {
	super(msg+" @ "+line+","+col);
	this.msg = msg;
	this.line = line;
	this.col = col;
    }

    public CompileException(String msg, Token lastToken) {
	this(msg, lastToken.line, lastToken.col);
    }
}
