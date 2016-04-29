import java.util.*;

public class Scope {
    private Map<String, Variable> varMap = new HashMap<String, Variable>();
    private Set<String> variables = new HashSet<String>();
    private int scopeNum;
    public Scope(int num) {
	scopeNum = num;
    }

    public Scope(int num, Scope src) {
	scopeNum = num;
	varMap.putAll(src.varMap);
    }

    public Set<String> getDeclared() {
	return variables;
    }

    public Variable getVariable(String name) {
	return varMap.get(name);
    }

    public void addVariable(Variable var) {
	if(hasName(var.name))
	    throw new CompileException("Variable '"+var.name+"' has already been declared in this scope",
				       var.nameToken);

	var.scopeNum = scopeNum;
	varMap.put(var.name, var);
	variables.add(var.name);
    }

    public boolean hasName(String varName) {
	return variables.contains(varName);
    }

    @Override
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("Symbol table ");
	buf.append(scopeNum);

	for(String var : varMap.keySet()) {
	    buf.append("\n");
	    buf.append(varMap.get(var));
	}

	return buf.toString();
    }
}

class Variable {
    public final Token nameToken;
    public final String name;
    public final String type;
    public final String value;
    public int scopeNum;

    public Variable(Token nameToken, String type) {
	this.nameToken = nameToken;
	this.name = nameToken.value;
	this.type = type;
	this.value = null;
    }

    public Variable(Token nameToken, String type, String value) {
	this.nameToken = nameToken;
	this.name = nameToken.value;
	this.type = type;
	this.value = value;
    }

    private static final String PREFIX="var";
    public String makeName() {
	StringBuffer buf = new StringBuffer(PREFIX);
	buf.append(scopeNum);
	buf.append(name);
	return buf.toString();
    }

    @Override
    public String toString() {
	StringBuffer buf = new StringBuffer();

	buf.append("name ");
	buf.append(name);
	buf.append("_");
	buf.append(scopeNum);
	buf.append(" type ");
	buf.append(type);
	if(value != null) {
	    buf.append(" value ");
	    buf.append("\"");
	    buf.append(value);
	    buf.append("\"");
	}
	
	return buf.toString();
    }
}
