import java.util.Set;

public class IRNode {
    enum Type {
	ADDI((n) -> arith("addi",n)),
	ADDF((n) -> arith("addr",n)),
	SUBI((n) -> arith("subi",n)),
	SUBF((n) -> arith("subr",n)),
	MULTI((n) -> arith("muli",n)),
	MULTF((n) -> arith("mulr",n)),
	DIVI((n) -> arith("divi",n)),
	DIVF((n) -> arith("divr",n)),
	STOREI((n) -> move(n.op1, n.res)),
	STOREF((n) -> move(n.op1, n.res)),
	GTI((n) -> cmpi(n.op1, n.op2)+op("jgt",n.res)),
	GEI((n) -> cmpi(n.op1, n.op2)+op("jge",n.res)),
	LTI((n) -> cmpi(n.op1, n.op2)+op("jlt",n.res)),
	LEI((n) -> cmpi(n.op1, n.op2)+op("jle",n.res)),
	NEI((n) -> cmpi(n.op1, n.op2)+op("jne",n.res)),
	EQI((n) -> cmpi(n.op1, n.op2)+op("jeq",n.res)),
	GTF((n) -> cmpr(n.op1, n.op2)+op("jgt",n.res)),
	GEF((n) -> cmpr(n.op1, n.op2)+op("jge",n.res)),
	LTF((n) -> cmpr(n.op1, n.op2)+op("jlt",n.res)),
	LEF((n) -> cmpr(n.op1, n.op2)+op("jle",n.res)),
	NEF((n) -> cmpr(n.op1, n.op2)+op("jne",n.res)),
	EQF((n) -> cmpr(n.op1, n.op2)+op("jeq",n.res)),
	JUMP((n) -> op("jmp", n.res)),
	LABEL((n) -> op("label", n.res)),
	READI((n) -> op("sys readi", n.res)),
	READF((n) -> op("sys readr", n.res)),
	WRITEI((n) -> op("sys writei", n.res)),
	WRITEF((n) -> op("sys writer", n.res)),
	WRITES((n) -> op("sys writes", n.res));

	public final Transformer transformer;
	private Type(Transformer t) {
	    transformer = t;
	}

	private Type() {
	    transformer = null;
	}

	private static String cmpi(String op1, String op2) {
	    return "cmpi "+op1+" "+op2+"\n";
	}

	private static String cmpr(String op1, String op2) {
	    return "cmpr "+op1+" "+op2+"\n";
	}

	private static String move(String from, String to) {
	    return "move "+from+" "+to+"\n";
	}

	private static String op(String op,String from, String to) {
	    return op+" "+from+" "+to+"\n";
	}

	private static String op(String op,String to) {
	    return op+" "+to+"\n";
	}
	private static String arith(String op, IRNode n) {
	    return move(n.op1,n.res)+op(op,n.op2,n.res);
	}
    }

    static interface Transformer {
	public String transform(IRNode n);
    }

    private Type type;
    private String op1, op2, res;
    public IRNode(Type instructionType, String op1, String op2, String res) {
	this.type = instructionType;
	this.op1 = op1;
	this.op2 = op2;
	this.res = res;
    }

    public IRNode(Type instructionType, String op1, String res) {
	this.type = instructionType;
	this.op1 = op1;
	this.res = res;
    }
    
    public IRNode(Type instructionType, String res) {
	this.type = instructionType;
	this.res = res;
    }

    private Scope scope;
    public IRNode(Scope scope) {
	this.scope = scope;
    }

    @Override
    public String toString() {
	if(scope != null)
	    return null;
	StringBuffer ret = new StringBuffer(type.toString());
	if(op1 != null) {
	    ret.append(" ");
	    ret.append(op1);

	    if(op2 != null) {
		ret.append(" ");
		ret.append(op2);
	    }
	}
	ret.append(" ");
	ret.append(res);
	return ret.toString();
    }

    public String translate() {
	if(scope != null) {
	    StringBuffer decls = new StringBuffer();
	    Set<String> declared = scope.getDeclared();
	    for(String varName : declared) {
		Variable var = scope.getVariable(varName);
		if(var.type.equals("STRING")) {
		    decls.append("str ");
		    decls.append(var.makeName());
		    decls.append(" \"");
		    decls.append(var.value);
		    decls.append("\"\n");
		} else {
		    decls.append("var ");
		    decls.append(var.makeName());
		    decls.append("\n");
		}
	    }
	    return decls.toString();
	} else {
	    op1 = tinyify(op1);
	    op2 = tinyify(op2);
	    res = tinyify(res);
	
	    return type.transformer.transform(this);
	}
    }

    public String tinyify(String in) {
	if(in == null)
	    return null;
	if(in.startsWith("$T"))
	    return "r"+in.substring(2);
	return in;
    }

    public static void main(String[] args) {
	System.out.println(new IRNode(Type.JUMP, "L2"));
	System.out.println(new IRNode(Type.STOREI, "$T1", "$T3"));
	System.out.println(new IRNode(Type.ADDI, "$T1", "$T2", "$T3"));
	System.out.println(new IRNode(Type.LTI, "$T1", "$T2", "L2"));
    }
}
