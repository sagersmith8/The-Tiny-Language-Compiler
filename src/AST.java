import java.util.*;

public class AST {
    private Stack<ASTNode> stack = new Stack<>();
    private Stack<Integer> posStack = new Stack<>();
    public enum Type {
	IntLiteral("IntLiteral"),
	FloatLiteral("FloatLiteral"),
	StringLiteral("StringLiteral"),
	Identifier("Identifier"),
	LessThan("LessThan"),
	GreaterThan("GreaterThan"),
	LessThanEqual("LessThanEqual"),
	GreaterThanEqual("GreaterThanEqual"),
	NotEqual("NotEqual"),
	Equal("Equal"),
	Addition("Addition"),
	Subtraction("Subtraction"),
	Multiplication("Multiplication"),
	Division("Division"),
	Assignment("Assignment"),
	Read("Read"),
	Write("Write"),
	If("If"),
	While("While"),
	Condition("Condition"),
	StringDeclaration("StringDeclaration"),
	VariableDeclaration("VariableDeclaration"),
	DeclarationList("DeclarationList"),
	StatementList("StatementList"),
	IntType("IntType"),
	FloatType("FloatType"),
	StringType("StringType"),
	VoidType("VoidType"),
	Parameter("Parameter"),
	ParameterList("ParameterList"),
	Function("Function"),
	FunctionList("FunctionList"),
	Program("Program");

	public final String string;
	private Type(String string) {
	    this.string = string;
	}
    }

    static class ASTNode {
	public final Type type;
	public final String value;
	public final List<ASTNode> children;
	public Scope scope;

	public ASTNode(Type type, String value) {
	    this.type = type;
	    this.value = value;
	    children = new ArrayList<ASTNode>();
	}

	public ASTNode(Type type) {
	    this.type = type;
	    this.value = null;
	    children = new ArrayList<ASTNode>();
	}

	public String toString() {
	    StringBuffer buf = new StringBuffer();
	    buf.append("<");
	    buf.append(type.string);
	    if(value != null) {
		buf.append(" ");
		buf.append(value);
	    }
	    if(children.size() > 0) {
		if(scope != null) {
		    buf.append(" table=\"");
		    buf.append(scope);
		    buf.append("\"");
		}
		buf.append(">\n");

		for(ASTNode child : children) {
		    buf.append(child.toString());
		}
		buf.append("</");
		buf.append(type.string);
		buf.append(">\n");
	    } else {
		if(scope != null) {
		    buf.append(" table=\"");
		    buf.append(scope);
		    buf.append("\"");
		}
		buf.append("/>\n");
	    }

	    return buf.toString();

	    /*public void generateCode(List<IRNode> irCode) {
		
		for(ASTNode child : children) {
		    generateCode(irCode);
		}
		}*/
	}
    }

    public ASTNode getRoot() {
	return stack.peek();
    }

    public void print() {
	for(ASTNode n : stack) {
	    System.out.println(n);
	}
    }

    public void setTopScope(Scope scope) {
	stack.peek().scope = scope;
    }

    public void buildNode(Type type) {
	stack.add(new ASTNode(type));
    }

    public void buildNode(Type type, String value) {
	stack.add(new ASTNode(type, value));
    }

    public void buildNode(Type type, String value, int numChildren) {
	stack.add(readChildren(new ASTNode(type, value), numChildren));
    }

    public void buildNode(Type type, int numChildren) {
	 stack.add(readChildren(new ASTNode(type), numChildren));
    }

    public void startMark() {
	posStack.push(stack.size());
    }

    public int endMark() {
	return stack.size() - posStack.pop();
    }

    private ASTNode readChildren(ASTNode node, int num) {
	for(int i = 0; i < num; i++) {
	    node.children.add(0, stack.pop());
	}
	return node;
    }
}
