import java.io.IOException;
import java.util.List;

public class ErrorPrinter {
    public static void printError(String fileName, CompileException c) {
	try {
	    List<String> lines = java.nio.file.Files.readAllLines(new java.io.File(fileName).toPath(),
								  java.nio.charset.Charset.forName("UTF-8"));

	    String line = lines.get(c.line-1).trim();
	    int col = c.col;

	    System.out.println("Line "+c.line+": "+c.msg);
	    System.out.println(line);
	    System.out.println(pointerStr(col));
	} catch(IOException i) {
	    throw new RuntimeException(i.getMessage());
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
