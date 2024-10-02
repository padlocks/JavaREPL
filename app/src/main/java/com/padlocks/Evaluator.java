package com.padlocks;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Evaluator {
	// State variables
	private static final Map<String, Class<?>> compiledClasses = new HashMap<>();
	private static final Map<String, Method> compiledMethods = new HashMap<>();
	private static final Map<String, Variable> storedVariables = new HashMap<>();

	public static void evaluateInput(String input) throws Exception {
		// Imports
		if (isImport(input)) {
			handleImport(input);
		// Classes
		} else if (isClass(input)) {
			evaluateClass(input);
		// Methods
		} else if (isMethod(input)) {
			evaluateMethod(input);
		// Expressions
		} else if (isExpression(input)) {
			evaluateExpression(input);
		// Statements
		} else {
			evaluateStatement(input);
		}
	}

	private static boolean isImport(String input) {
		return input.startsWith("import ");
	}

	private static void handleImport(String input) throws Exception {
		// Crude implementation of import handling, revisit this later
		String className = input.substring(input.lastIndexOf(' ') + 1, input.length() - 1).trim();
		String fileName = className.replace('.', '/') + ".java";
		compile(fileName, input);
		loadCompiledClass(className);
	}

	private static boolean isExpression(String input) {
		return !input.endsWith(";");
	}

	private static boolean isClass(String input) {
		return input.startsWith("class ") || 
		input.startsWith("public class ") || 
		input.startsWith("private class ") || 
		input.startsWith("protected class ");
	}

	private static boolean isMethod(String input) {
		return (input.startsWith("public ") || input.startsWith("private ") || input.startsWith("protected ")) && input.contains("(") && input.contains(")");
	}

	private static Object evaluateExpression(String input) throws Exception {
		String className = "ExpressionEvaluator";
		String methodName = "eval";
		StringBuilder code = new StringBuilder();
		code.append("import java.lang.*; public class ").append(className).append(" { ");
		code = injectStoredVariables(code);
		code.append("public static Object ").append(methodName).append("() { ").append("return ").append(input.replace(";", "")).append("; } }");

		
		Object result = compileAndExecute(className, methodName, code.toString());
		if (result != null) {
			System.out.println("Result: " + result);
			return result;
		}

		return null;
	}

	private static void evaluateStatement(String input) throws Exception {
		if (input.contains("=")) {
			handleVariableDeclaration(input);
		} else if (input.contains("(") && input.contains(")")) {
			handleMethodInvocation(input);
		} else {
			// It's an expression ending with a semicolon
			evaluateExpression(input);
		}
	}

	private static void handleVariableDeclaration(String input) throws Exception {
		String[] tokens = input.split("=");
		String variableName;
		String variableType = null;
		String value = tokens[1].replace(";", "").trim();

		// Check if it's a declaration or an assignment
		// This is disgusting but it works to differentiate between declaration and assignment
		if (tokens[0].split(" ").length == 2) {
			// Declaration
			variableName = tokens[0].split(" ")[1].trim();
			variableType = tokens[0].split(" ")[0].trim();
		} else {
			// Assignment
			variableName = tokens[0].trim();
		}

		// Parse the value and store the variable
		Object parsedValue = evaluateVariable(value);

		// If it's a declaration, create a new variable
		if (variableType != null) {
			Variable newVariable = new Variable(variableName, variableType, parsedValue);
			storedVariables.put(variableName, newVariable);
		} else {
			// If it's an assignment, update the existing variable
			Variable existingVariable = storedVariables.get(variableName);
			if (existingVariable != null) {
				existingVariable.setValue(parsedValue);
			} else {
				throw new RuntimeException("Variable " + variableName + " not declared.");
			}
		}
	}

	private static Object evaluateVariable(String value) throws Exception {
		// Check if variable is equal to a method call
		if (value.contains("new ")) {
			return handleClassInstantiation(value);
		}
		else if (value.contains("(") && value.contains(")")) {
			return handleMethodInvocation(value);
		} else {
			return evaluateExpression(value);
		}
	}

	private static Object handleClassInstantiation(String input) throws Exception {
		String className = input.substring(input.indexOf("new ") + 4, input.indexOf("(")).trim();
		String[] arguments = input.substring(input.indexOf('(') + 1, input.indexOf(')')).split(",");
		Object[] args = arguments.length == 1 && arguments[0].trim().isEmpty() 
			? new Object[0] 
			: Arrays.stream(arguments)
				.map(arg -> parseArgument(arg.trim()))
				.toArray();

		// Infer the argument types and handle conversion of wrapper types to primitives
    	Class<?>[] argTypes = Arrays.stream(args)
            .map(Object::getClass)
			.map(Evaluator::convertToPrimitive)
            .toArray(Class<?>[]::new);

		// Check if the class is already compiled locally
		Class<?> clazz = compiledClasses.containsKey(className) 
			? compiledClasses.get(className) 
			: Class.forName(className);

		// Get the constructor with the appropriate argument types
		Constructor<?> constructor = clazz.getDeclaredConstructor(argTypes);
		// If the constructor is not public, make it accessible
		constructor.setAccessible(true);
		return constructor.newInstance(args);
	}

	// Helper method to parse arguments dynamically
	private static Object parseArgument(String arg) {
		// Parse basic types like Integer, Double, Boolean, etc.
		// Integer
		if (arg.matches("-?\\d+")) {
			return Integer.valueOf(arg);
		// Double
		} else if (arg.matches("-?\\d+\\.\\d+")) {
			return Double.valueOf(arg);
		// Boolean
		} else if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false")) {
			return Boolean.valueOf(arg);
		// Long
		} else if (arg.matches("-?\\d+L")) {
			return Long.valueOf(arg.substring(0, arg.length() - 1));
		// Float
		} else if (arg.matches("-?\\d+\\.\\d+F")) {
			return Float.valueOf(arg.substring(0, arg.length() - 1));
		// Short
		} else if (arg.matches("-?\\d+S")) {
			return Short.valueOf(arg.substring(0, arg.length() - 1));
		// Byte
		} else if (arg.matches("-?\\d+B")) {
			return Byte.valueOf(arg.substring(0, arg.length() - 1));
		// Character
		} else if (arg.startsWith("'") && arg.endsWith("'")) {
			return arg.charAt(1);
		// Handle strings, stripped of quotes
		} else if (arg.startsWith("\"") && arg.endsWith("\"")) {
			return arg.substring(1, arg.length() - 1);
		} else {
			// Try to resolve the argument as a class name (enum, or another type)
			try {
				Class<?> clazz = Class.forName(arg);
				return clazz;
			} catch (ClassNotFoundException e) {
				// If not found, assume it is a string argument
				return arg;
			}
		}
	}

	// Helper method to convert wrapper types to their primitive types
	private static Class<?> convertToPrimitive(Class<?> clazz) {
		if (clazz == Integer.class) {
			return int.class;
		} else if (clazz == Double.class) {
			return double.class;
		} else if (clazz == Boolean.class) {
			return boolean.class;
		} else if (clazz == Long.class) {
			return long.class;
		} else if (clazz == Float.class) {
			return float.class;
		} else if (clazz == Short.class) {
			return short.class;
		} else if (clazz == Byte.class) {
			return byte.class;
		} else if (clazz == Character.class) {
			return char.class;
		}

		// Return the class itself if it's not a wrapper type
		return clazz;
	}

	private static Object handleMethodInvocation(String input) throws Exception {
		// Find the position of the dot to separate variable and method
		int dotIndex = input.indexOf(".");
		
		// If there's a dot, it's a method call on an instance variable
		if (dotIndex != -1) {
			String variableName = input.substring(0, dotIndex).trim();
			String methodCall = input.substring(dotIndex + 1).trim();
			
			// Extract the method name and arguments
			String methodName = methodCall.substring(0, methodCall.indexOf("("));
			String argsString = methodCall.substring(methodCall.indexOf("(") + 1, methodCall.indexOf(")"));
			String[] arguments = argsString.split(",");
			
			// Retrieve the instance from stored variables
			Object instance = storedVariables.get(variableName).getValue();
			Object[] args = arguments.length == 1 && arguments[0].trim().isEmpty() 
				? new Object[0] 
				: Arrays.stream(arguments)
					.map(arg -> storedVariables.get(arg.trim()).getValue())
					.toArray();
			
			// Find the method in the instance's class
			Method method = findMethod(methodName, args, instance.getClass());
			
			// Invoke the method on the instance
			if (method != null) {
				Object result = method.invoke(instance, args);
				System.out.println("Result: " + result);
				return result;
			}
		} else {
			// Extract the method name
			String methodName = input.replace("(", "").replace(")", "").replace(";", "");
			
			// Extract arguments between parentheses and split them by comma
			String[] arguments = input.substring(input.indexOf('(') + 1, input.indexOf(')')).split(",");
			Object[] args = arguments.length == 1 && arguments[0].trim().isEmpty() 
				? new Object[0] 
				: Arrays.stream(arguments)
					.map(arg -> storedVariables.get(arg.trim()).getValue())
					.toArray();

			// Find the method based on the name and arguments
			Method method = findMethod(methodName, args, null);
			
			if (method != null) {
				Class<?> declaringClass = method.getDeclaringClass();
				
				// Check if the method is static
				if (Modifier.isStatic(method.getModifiers())) {
					// Static method: invoke without instance
					Object result = method.invoke(null, args);
					System.out.println("Result: " + result);
					return result;
				} else {
					// Non-static method: instantiate the class and invoke the method on the instance
					Object instance = declaringClass.getDeclaredConstructor().newInstance();
					Object result = method.invoke(instance, args);
					System.out.println("Result: " + result);
					return result;
				}
			} else {
				// If method not found, dynamically compile and execute the code
				String className = "DynamicMethodEvaluator";
				String code = "import java.lang.*; public class " + className + " { public static void eval() { " + input + " } }";
				return compileAndExecute(className, "eval", code);
			}
		}
		
		// No method found
		return null;
	}

	private static Method findMethod(String methodName, Object[] args, Class<?> clazz) {
		if (clazz == null) {
			Method method = compiledMethods.get("MethodEvaluator." + methodName);
			if (method == null) {
				for (Class<?> importedClass : compiledClasses.values()) {
					try {
						method = importedClass.getMethod(methodName, Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new));
						break;
					} catch (NoSuchMethodException e) {
						// Method not found in this class, continue searching
					}
				}
			}
			return method;
		}
		else {
			try {
				return clazz.getMethod(methodName, Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new));
			} catch (NoSuchMethodException e) {
				return null; // Method not found
			}
		}
	}

	private static void evaluateMethod(String input) throws Exception {
		String className = "MethodEvaluator";
		String methodName = input.substring(0, input.indexOf('(')).trim();
		methodName = methodName.substring(methodName.lastIndexOf(' ') + 1);
		
		// Create method code, injecting stored variables
		StringBuilder methodCode = new StringBuilder();
		methodCode = injectStoredVariables(methodCode);
		methodCode.append(input);
		
		// Compile and execute the method
		String code = "import java.lang.*; public class " + className + " { " + methodCode.toString() + " }";
		compile(className + ".java", code);
		loadCompiledClass(className);
		Method method = compiledClasses.get(className).getMethod(methodName);
		compiledMethods.put(className + "." + methodName, method);
	}

	private static StringBuilder injectStoredVariables(StringBuilder methodCode) {
		// Inject stored variables into the method code
		for (Map.Entry<String, Variable> entry : storedVariables.entrySet()) {
			methodCode.append("static ").append(entry.getValue().getType()).append(" ").append(entry.getKey())
					.append(" = ").append(entry.getValue().getValue()).append("; ");
		}
		return methodCode;
	}

	private static void evaluateClass(String input) throws Exception {
		String className = input.substring(input.indexOf("class") + 5, input.indexOf("{")).trim();
		compile(className + ".java", input.trim());
		loadCompiledClass(className);
	}

	private static void compile(String fileName, String code) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		File sourceFile = new File(fileName);
		try (Writer writer = new FileWriter(sourceFile)) {
			writer.write(code);
		}
		Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
		boolean compilationSuccess = compiler.getTask(null, fileManager, null, null, null, fileObjects).call();
		if (!compilationSuccess) {
			throw new RuntimeException("Compilation failed.");
		}
	}

	private static Object compileAndExecute(String className, String methodName, String code) throws Exception {
		compile(className + ".java", code);
		loadCompiledClass(className);
		Method method = compiledClasses.get(className).getMethod(methodName);
		return method.invoke(null);
	}

	private static void loadCompiledClass(String className) throws Exception {
		URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("").toURI().toURL()});
		Class<?> compiledClass = Class.forName(className, true, classLoader);
		compiledClasses.put(className, compiledClass);
		for (Method method : compiledClass.getDeclaredMethods()) {
			compiledMethods.put(className + "." + method.getName(), method);
		}
	}
}
