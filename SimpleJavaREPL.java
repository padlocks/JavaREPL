import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import javax.tools.*;

public class SimpleJavaREPL {

  public static void main(String[] args) throws Exception {
    Scanner scanner = new Scanner(System.in);
    String input;

    System.out.println("Welcome to the Simple Java REPL. Type 'exit' to quit.");

    // Execution Loop
    while (true) {
      // Prompt the user for input
      System.out.print(">> ");
      input = scanner.nextLine().trim();

      // Check if the user wants to exit the REPL
      if (input.equalsIgnoreCase("exit")) {
        System.out.println("Exiting REPL...");
        break;
      }

      // Check if the input is a block of code
      if (input.endsWith("{")) {
        StringBuilder codeBlock = new StringBuilder(input);
        while (!input.endsWith("}")) {
          System.out.print("... ");
          input = scanner.nextLine().trim();
          codeBlock.append("\n").append(input);
        }
        input = codeBlock.toString();
      }

      try {
        // Evaluate the input expression or code block
        evaluateExpression(input);
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
      }
    }

    scanner.close();
  }

  // Evaluate the input expression and print the result
  private static void evaluateExpression(String input) throws Exception {
    String className = "ExpressionEvaluator";
    String methodName = "eval";

    // Wrap the user's input in a class and method to compile
    String code = "import java.lang.*;" + "public class " + className + " {" +
        "    public static Object " + methodName + "() {" +
        "        return " + input + ";" + // Treat input as a return statement
        "    }" +
        "}";

    // Compile and execute the code
    Object result = compileAndExecute(className, methodName, code);

    // Print the result, if any
    if (result != null) {
      System.out.println("Result: " + result);
    }
  }

  // Compiles and runs the code dynamically, then invokes the method using
  // reflection
  private static Object compileAndExecute(String className, String methodName, String code) throws Exception {
    // Create a JavaCompiler instance
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    // Write the source code to a temporary file
    File sourceFile = new File(className + ".java");
    try (Writer writer = new FileWriter(sourceFile)) {
      writer.write(code);
    }

    // Compile the source file
    Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
    boolean compilationSuccess = compiler.getTask(null, fileManager, null, null, null, fileObjects).call();

    if (!compilationSuccess) {
      throw new RuntimeException("Compilation failed.");
    }

    // Load and execute the compiled class
    URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });
    Class<?> compiledClass = Class.forName(className, true, classLoader);
    Method method = compiledClass.getMethod(methodName);

    // Invoke the method and return the result
    return method.invoke(null);
  }
}
