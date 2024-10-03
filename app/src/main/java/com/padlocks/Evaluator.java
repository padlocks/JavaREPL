package com.padlocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import com.padlocks.Compiler;

public class Evaluator {
	private static final State state = new State();
	private static final Parser parser = new Parser();
	private static final Compiler compiler = new Compiler(state);

	public static void evaluateInput(String input) throws Exception {
		try {
			// Imports
			if (parser.isImport(input)) {
				handleImport(input);
			// Classes
			} else if (parser.isClass(input)) {
				evaluateClass(input);
			// Methods
			} else if (parser.isMethod(input)) {
				evaluateMethod(input);
			// Expressions
			} else if (parser.isExpression(input)) {
				evaluateExpression(input);
			// Statements
			} else {
				evaluateStatement(input);
			}
		} catch (Exception e) {
			// Print the exception message and local state
			System.out.println("\n\nError: " + e.getMessage());

			System.out.println(state);
			System.out.println();

			e.printStackTrace();
		}
	}

	private static void handleImport(String input) throws Exception {
		// Determine if the import is a local import or a java package import
		String className = input.substring(input.lastIndexOf(' ') + 1, input.length() - 1).trim();
		if (className.startsWith("java.")) {
			// Java package import, no need to compile
			// System.out.println("Imported Java package: " + className);
		} else {
			// Local import, compile the class
			String fileName = className.replace('.', '/') + ".java";
			compiler.compile(fileName, input);
			compiler.loadCompiledClass(className, input);
		}

		// Store import for injection
		state.addStoredImport(input);
	}

	private static Object evaluateExpression(String input) throws Exception {
		String className = "ExpressionEvaluator";
		String methodName = "eval";
		StringBuilder code = new StringBuilder();
		code.append("import java.lang.*; public class ").append(className).append(" { ");
		code = injectStoredVariables(code);
		code.append("public static Object ").append(methodName).append("() { ").append("return ").append(input.replace(";", "")).append("; } }");

		
		Object result = compiler.compileAndExecute(className, methodName, code.toString());
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
			Variable newVariable = new Variable(variableName, variableType, parsedValue, input);
			state.addStoredVariable(variableName, newVariable);
		} else {
			// If it's an assignment, update the existing variable
			Variable existingVariable = state.getStoredVariable(variableName);
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
				.map(arg -> parser.parseArgument(arg.trim()))
				.toArray();

		// Infer the argument types and handle conversion of wrapper types to primitives
    	Class<?>[] argTypes = Arrays.stream(args)
            .map(Object::getClass)
			.map(parser::convertToPrimitive)
            .toArray(Class<?>[]::new);

		// Check if the class is already compiled locally
		Class<?> clazz = state.getCompiledClasses().containsKey(className) 
			? state.getCompiledClass(className) 
			: Class.forName(className);

		// Get the constructor with the appropriate argument types
		Constructor<?> constructor = clazz.getDeclaredConstructor(argTypes);
		// If the constructor is not public, make it accessible
		constructor.setAccessible(true);
		return constructor.newInstance(args);
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
			Object[] args = arguments.length == 1 && arguments[0].trim().isEmpty() 
				? new Object[0] 
				: Arrays.stream(arguments)
					.map(arg -> parser.parseArgument(arg.trim()))
					.toArray();

			if (state.getStoredVariable(variableName) == null) {
				Object result = evaluateMethodInvocationDynamically(methodName, args, input);
				System.out.println("Result: " + result);
				return result;
			}
			
			// Retrieve the instance from stored variables
			Object instance = state.getStoredVariable(variableName).getValue();
			
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
					.map(arg -> parser.parseArgument(arg.trim()))
					.toArray();

			Object result = evaluateMethodInvocationDynamically(methodName, args, input);
			System.out.println("Result: " + result);
			return result;
		}
		
		// No method found
		return null;
	}

	private static Object evaluateMethodInvocationDynamically(String methodName, Object[] args, String input) throws Exception {
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
				StringBuilder code = new StringBuilder();
				code.append("import java.lang.*; public class ").append(className).append(" { ");
				code = injectStoredVariables(code);
				code.append("public static")
					.append(input.contains("return") ? " Object " : " void ")
					.append("eval() { ")
					.append(input)
					.append(" } }");
				
				code = injectImports(code);
				return compiler.compileAndExecute(className, "eval", code.toString());
			}
	}

	private static Method findMethod(String methodName, Object[] args, Class<?> clazz) {
		if (clazz == null) {
			Method method = state.getCompiledMethod("MethodEvaluator." + methodName);
			if (method == null) {
				for (Class<?> importedClass : state.getCompiledClasses().values()) {
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
		
		// Extract the parameter types
		String paramString = input.substring(input.indexOf('(') + 1, input.indexOf(')')).trim();
		Class<?>[] parameterTypes = parser.extractParameterTypes(paramString);

		// Create method code, injecting stored variables
		StringBuilder methodCode = new StringBuilder();
		methodCode = injectStoredVariables(methodCode);
		methodCode.append(input);
		
		// Compile and execute the method
		StringBuilder code = new StringBuilder();
		code.append("import java.lang.*; public class ").append(className).append(" { ")
			.append(methodCode.toString()).append(" }");
		code = injectImports(code);

		compiler.compile(className + ".java", code.toString());
		compiler.loadCompiledClass(className, code.toString());
		Method method = state.getCompiledClass(className).getMethod(methodName, parameterTypes);
		state.addCompiledMethod(className + "." + methodName, method);
	}

	private static StringBuilder injectStoredVariables(StringBuilder methodCode) {
		// Inject stored variables into the method code
		for (Map.Entry<String, Variable> entry : state.getStoredVariables().entrySet()) {
			methodCode.append("static ").append(entry.getValue().getInput());
		}
		return methodCode;
	}

	private static void evaluateClass(String input) throws Exception {
		String className = input.substring(input.indexOf("class") + 5, input.indexOf("{")).trim();
		className = className.split(" ")[0];

		StringBuilder code = new StringBuilder();
		code = code.append(input);

		// Inject stored imports
		code = injectImports(code);
		
		// Check for extends or implements
		// String extendsClassName = input.contains("extends") ? input.substring(input.indexOf("extends") + 7, input.indexOf("{")).trim() : null;
		// String implementsInterfaceName = input.contains("implements") ? input.substring(input.indexOf("implements") + 10, input.indexOf("{")).trim() : null; 
		
		// Compile the class
		compiler.compile(className + ".java", code.toString().trim());
		compiler.loadCompiledClass(className, code.toString().trim());
	}

	private static StringBuilder injectImports(StringBuilder code) {
		// Inject stored imports into the class code
		for (String importStatement : state.getStoredImports()) {
			code.insert(0, importStatement + "\n");
		}

		return code;
	}
}
