package com.padlocks;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class SimpleJavaREPL {

  public static void main(String[] args) throws Exception {
    Terminal terminal = TerminalBuilder.builder().system(true).build();
    Completer completer = new StringsCompleter(getKeywords());
    LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer).build();
    String input;

    System.out.println("Welcome to the Simple Java REPL. Type 'exit' to quit. Use tab while typing for autocomplete.");

    // Execution Loop
    while (true) {
      // Prompt the user for input
      input = reader.readLine(">> ").trim();

      // Check if the user wants to exit the REPL
      if (input.equalsIgnoreCase("exit")) {
        System.out.println("Exiting REPL...");
        break;
      }

      // Check if the input is a block of code
      if (input.endsWith("{")) {
        StringBuilder codeBlock = new StringBuilder(input);
        int openBraces = 1;
        while (openBraces > 0) {
          String prompt = "... ".repeat(openBraces);
          input = reader.readLine(prompt).trim();
          codeBlock.append("\n").append(input);
          openBraces += countOccurrences(input, '{');
          openBraces -= countOccurrences(input, '}');
        }
        input = codeBlock.toString();
      }

      try {
        // Evaluate the input expression, statement or code block
        evaluateInput(input);
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  // Evaluate the input and print the result
  private static void evaluateInput(String input) throws Exception {
    if (isClass(input)) {
      evaluateClass(input);
    } else if (isExpression(input)) {
      evaluateExpression(input);
    } else {
      evaluateStatement(input);
    }
  }

  // Check if the input is an expression
  private static boolean isExpression(String input) {
    return !input.endsWith(";");
  }

  // Check if the input is a class or a collection of classes
  private static boolean isClass(String input) {
    return input.startsWith("class ") || input.startsWith("public class ");
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

  // Evaluate the input statement
  private static void evaluateStatement(String input) throws Exception {
    String className = "StatementEvaluator";
    String methodName = "eval";

    // Wrap the user's input in a class and method to compile
    String code = "import java.lang.*;" + "public class " + className + " {" +
        "    public static void " + methodName + "() {" +
        input + // Treat input as a statement
        "    }" +
        "}";

    // Compile and execute the code
    compileAndExecute(className, methodName, code);
  }

  // Evaluate the input class or collection of classes
  private static void evaluateClass(String input) throws Exception {
    // Split the input into individual classes
    String[] classes = input.split("(?<=\\})");

    // Write each class into its own file and compile it
    for (String classCode : classes) {
      String className = classCode.substring(classCode.indexOf("class") + 5, classCode.indexOf("{")).trim();
      String fileName = className + ".java";

      // Compile the class
      compile(fileName, classCode.trim());
    }
  }

  // Only compiles code
  private static void compile(String fileName, String code) throws Exception {
    // Create a JavaCompiler instance
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    // Write the source code to a temporary file
    File sourceFile = new File(fileName);
    try (Writer writer = new FileWriter(sourceFile)) {
      writer.write(code);
    }

    // Compile the source file
    Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
    boolean compilationSuccess = compiler.getTask(null, fileManager, null, null, null, fileObjects).call();

    if (!compilationSuccess) {
      throw new RuntimeException("Compilation failed.");
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

  // Returns a list of keywords for code completion
  private static List<String> getKeywords() {
    return Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
        "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "System.out.println", "exit");
  }

  // Count occurrences of a character in a string
  private static int countOccurrences(String str, char ch) {
    int count = 0;
    for (char c : str.toCharArray()) {
      if (c == ch) {
        count++;
      }
    }
    return count;
  }
}
