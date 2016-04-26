import java.util.*;

public class Scope {
    private List<String> variables = new LinkedList<String>();
    private Map<String, Variable> varMap = new HashMap<String, Variable>();

    private String name;
    
    public Scope(String name) {
	this.name = name;
    }

    public void addVariable(Variable var) {
	if(hasName(var.name))
	    throw new CompileException("Variable '"+var.name+"' has already been declared in this scope",
					   var.nameToken);
	
	varMap.put(var.name, var);
	variables.add(var.name);
    }

    public boolean hasName(String varName) {
	return varMap.containsKey(varName);
    }

    @Override
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("Symbol table ");
	buf.append(name);

	for(String var : variables) {
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

    @Override
    public String toString() {
	StringBuffer buf = new StringBuffer();

	buf.append("name ");
	buf.append(name);
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
