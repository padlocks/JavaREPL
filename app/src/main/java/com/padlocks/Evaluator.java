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
			// Accumulate the input code
			state.updateCode(new StringBuilder(input));
			executeAccumulatedCode(input);
		} catch (Exception e) {
			// Print the exception message and local state
			System.out.println("\n\nError: " + e.getMessage());
			System.out.println(state);
			System.out.println();
			e.printStackTrace();
		}
	}

	private static void handleJavaFile(String input, Boolean reEvaluate) throws Exception {
		// Split the input into lines
		String[] lines = input.split("\n");

		// Process each line
		for (String line : lines) {
			// Check if the line is an import statement
			if (parser.isImport(line)) {
				handleImport(line, false);
				// Remove the import statement from the input
				input = input.replace(line, "");
			}
		}

		if (reEvaluate) evaluateInput(input.trim());
	}

	private static void handleImport(String input, Boolean reEvaluate) throws Exception {
		// Check if the input has a class inside
		if (input.contains("class")) {
			handleJavaFile(input, reEvaluate);
			return;
		}

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

	private static void handleVariableDeclaration(String input) throws Exception {
		String[] tokens = input.split("=");
		String variableName;
		String variableType = null;
		String value = tokens[1].replace(";", "").trim();

		// Check if it's a declaration or an assignment
		if (tokens[0].split(" ").length == 2) {
			variableName = tokens[0].split(" ")[1].trim();
			variableType = tokens[0].split(" ")[0].trim();
		} else {
			variableName = tokens[0].trim();
		}

		Object parsedValue = evaluateVariable(value);

		// If it's a declaration, check if the variable already exists
		Variable existingVariable = state.getStoredVariable(variableName);
		if (variableType != null) {
			if (existingVariable == null) {
				Variable newVariable = new Variable(variableName, variableType, parsedValue, input);
				state.addStoredVariable(variableName, newVariable);
			} else {
				// Update the value of the existing variable
				existingVariable.setValue(parsedValue);
			}
		} else if (existingVariable != null) {
			existingVariable.setValue(parsedValue);
		} else {
			throw new RuntimeException("Variable " + variableName + " not declared.");
		}
	}

	private static Object evaluateVariable(String value) throws Exception {
		// Check if variable is equal to a method call
		if (value.contains("new ")) {
			return handleClassInstantiation(value);
		} else if (value.contains("(") && value.contains(")")) {
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
					.map(arg -> {
						Variable storedVar = state.getStoredVariable(arg.trim());
						return storedVar != null ? storedVar.getValue() : parser.parseArgument(arg.trim());
					})
					.toArray();

			// Static method call
			if (state.getStoredVariable(variableName) == null) {
				Object result = evaluateMethodInvocationDynamically(methodName, args, input);
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
			String methodName = input.split("\\(")[0].replace("return ", "").trim();

			// Extract arguments between parentheses and split them by comma
			String[] arguments = input.substring(input.indexOf('(') + 1, input.indexOf(')')).split(",");
			Object[] args = arguments.length == 1 && arguments[0].trim().isEmpty() 
				? new Object[0] 
				: Arrays.stream(arguments)
					.map(arg -> parser.parseArgument(arg.trim()))
					.toArray();

			Object result = evaluateMethodInvocationDynamically(methodName, args, input);
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
		} else {
			try {
				return clazz.getMethod(methodName, Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new));
			} catch (NoSuchMethodException e) {
				return null; // Method not found
			}
		}
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

	// Method to execute the accumulated code
	public static void executeAccumulatedCode(String input) throws Exception {
		StringBuilder code = state.getCode();
		if (!code.toString().isEmpty()) {
			// Check for clear() and clearAll() methods
			if (input.equals("clear();")) {
				String className = "Eval";
				// Clear main method body
				if (state.getCompiledClass(className) != null) {
					String evalSource = state.getClassSource(className);
					StringBuilder newEvalCode = new StringBuilder();
						
					// Line by line
					String[] lines = evalSource.split("\n");
					boolean mainMethodFound = false;
					for (String line : lines) {
						if (line.contains("public static void main(String[] args) {")) {
							mainMethodFound = true;
							newEvalCode.append(line).append("\n");
						}
						if (mainMethodFound && line.contains("}")) {
							mainMethodFound = false;
							newEvalCode.append("}\n");
							continue;
						}
						if (!mainMethodFound) {
							newEvalCode.append(line).append("\n");
						}
					}

					// Compile and execute the Eval class
					compiler.compile(className + ".java", newEvalCode.toString());
					compiler.loadCompiledClass(className, newEvalCode.toString());
				}
				System.out.println("Main method body cleared.");
				return;
			} else if (input.equals("clearAll();")) {
				state.clear();
				// Delete ./tmp directory
				compiler.deleteCompiledFiles();
				System.out.println("All stored variables, imports, classes, and methods cleared.");
				return;
			}

			if (parser.isImport(input)) {
				code = parser.organizeCode(code);
				state.updateCode(code);

				// Split code so that each class is compiled in its own file
				// Imports should be handled first and injected into each class
				handleImport(code.toString(), false);

				// Remove imports from the code
				code = new StringBuilder(code.toString().replaceAll("import .*;\n", "").trim());
				
				if (parser.isClass(code.toString())) {
					// Split the code by class
					String[] classes = parser.separateClasses(code.toString());
					for (String classCode : classes) {
						evaluateClass(classCode);
					}
				}
			} else if (parser.isClass(input)) {
				// Split the code by class
				String[] classes = parser.separateClasses(code.toString());
				for (String classCode : classes) {
					evaluateClass(classCode);
				}
			} else if (parser.isStaticVariable(input)) {
				// Add variable to state
				handleVariableDeclaration(input);
				if (state.getCompiledClass("Eval") == null) {
					// Generate eval class with main method
					StringBuilder evalCode = new StringBuilder();
					evalCode.append("public class Eval {\n");
					// Inject stored variables
					evalCode = injectStoredVariables(evalCode);
					// Inject the new static variable
					evalCode.append(input).append("\n");
					evalCode.append("public static void main(String[] args) {\n");
					evalCode.append("}\n");
					evalCode.append("}\n");
					// Inject imports
					evalCode = injectImports(evalCode);

					// Compile the Eval class
					compiler.compile("Eval.java", evalCode.toString());
					compiler.loadCompiledClass("Eval", evalCode.toString());
				} else {
					// Inject the static variable into the class, before the main method
					String evalSource = state.getClassSource("Eval");
					StringBuilder newEvalCode = new StringBuilder();

					// Line by line
					String[] lines = evalSource.split("\n");
					for (String line : lines) {
						if (line.contains("public class Eval {")) {
							newEvalCode.append(line).append("\n");
							// Inject stored variables only if they are new and not already there
							for (Map.Entry<String, Variable> entry : state.getStoredVariables().entrySet()) {
								if (!evalSource.contains(entry.getValue().getInput())) {
									newEvalCode.append("static ").append(entry.getValue().getInput());
								}
							}
							// Inject the new static variable
							newEvalCode.append(input).append("\n");
						} else {
							newEvalCode.append(line).append("\n");
						}
					}
				}
			} else if (parser.isMethod(input)) {
				String className = "Eval";

				// Check if Eval class already exists
				if (state.getCompiledClass(className) != null) {
					// Inject the method into the existing Eval class before the main method
					String evalSource = state.getClassSource(className);
					StringBuilder newEvalCode = new StringBuilder();
						
					// Line by line
					String[] lines = evalSource.split("\n");
					for (String line : lines) {
						if (line.contains("public static void main(String[] args) {")) {
							// Make sure the new method is static
							String newMethod = parser.makeStatic(input);
							// Inject the new method before the main method
							newEvalCode.append(newMethod).append("\n");
						}
						newEvalCode.append(line).append("\n");
					}

					// Compile and execute the Eval class
					compiler.compile(className + ".java", newEvalCode.toString());
					compiler.loadCompiledClass(className, newEvalCode.toString());
				} else {
					// Wrap the method in the Eval class, generate empty main function
					StringBuilder classCode = new StringBuilder();
					classCode.append("public class ").append(className).append(" { ");
					classCode = injectStoredVariables(classCode);
					// Make the method static
					String newMethod = parser.makeStatic(input);
					// Inject the new method
					classCode.append(newMethod);
					classCode.append("public static void main(String[] args) {\n").append("}\n}");
					classCode = injectImports(classCode);

					// Compile the class
					compiler.compile(className + ".java", classCode.toString());
					compiler.loadCompiledClass(className, classCode.toString());
				}
			} else {
				// Check if there is a main method
				if (state.getCompiledClass("Eval") == null) {
					// Generate eval class with main method
					StringBuilder evalCode = new StringBuilder();
					evalCode.append("public class Eval {\n");
					// Inject stored variables
					evalCode = injectStoredVariables(evalCode);
					evalCode.append("public static void main(String[] args) {\n");

					if (parser.isExpression(input)) {
						evaluateExpression(input);
					} else {
						evalCode.append(input).append("\n");
					}

					evalCode.append("}\n");
					evalCode.append("}\n");
					// Inject imports
					evalCode = injectImports(evalCode);

					// Compile and execute the Eval class
					compiler.compile("Eval.java", evalCode.toString());
					compiler.loadCompiledClass("Eval", evalCode.toString());
					Method mainMethod = state.getCompiledClass("Eval").getMethod("main", String[].class);
					mainMethod.invoke(null, (Object) new String[0]);
				} else {
					// Inject the code into the main method
					String evalSource = state.getClassSource("Eval");
					StringBuilder newEvalCode = new StringBuilder();
						
					// Line by line
					String[] lines = evalSource.split("\n");
					boolean mainMethodFound = false;
					boolean variablesInjected = false;
					for (String line : lines) {
						if (!variablesInjected && line.contains("public class Eval {")) {
							newEvalCode.append(line).append("\n");
							// Inject stored variables only if they are new and not already there
							for (Map.Entry<String, Variable> entry : state.getStoredVariables().entrySet()) {
								if (!evalSource.contains(entry.getValue().getInput())) {
									newEvalCode.append("static ").append(entry.getValue().getInput());
								}
							}
							variablesInjected = true;
						} else if (line.contains("public static void main(String[] args) {")) {
							mainMethodFound = true;
							newEvalCode.append(line).append("\n");
							// Inject old main body
							String oldMainBody = parser.extractMainMethodBody(evalSource);
							newEvalCode.append(oldMainBody).append("\n");
							// Inject the new code
							newEvalCode.append(code).append("\n");

						} else if (mainMethodFound && !line.contains("}")) {
							// Skip the old main method body
							continue;
						} else {
							newEvalCode.append(line).append("\n");
						}
					}

					// Compile and execute the Eval class
					compiler.compile("Eval.java", newEvalCode.toString());
					compiler.loadCompiledClass("Eval", newEvalCode.toString());
					Method mainMethod = state.getCompiledClass("Eval").getMethod("main", String[].class);
					mainMethod.invoke(null, (Object) new String[0]);
				}
			}
		}
	}
}
