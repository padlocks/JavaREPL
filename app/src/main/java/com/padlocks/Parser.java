package com.padlocks;

import java.util.Arrays;
import java.util.List;

public class Parser {
    public boolean isImport(String input) {
        return input.startsWith("import ");
    }

    public boolean isClass(String input) {
        return input.startsWith("class ") ||
               input.startsWith("public class ") ||
               input.startsWith("private class ") ||
               input.startsWith("protected class ");
    }

    public boolean isMethod(String input) {
        // Assume a method starts with access modifier and contains '(' and ')'
        return (input.startsWith("public ") ||
                input.startsWith("private ") ||
                input.startsWith("protected ")) &&
               input.contains("(") && input.contains(")");
    }

    public boolean isExpression(String input) {
        return !input.endsWith(";");
    }

	public boolean isMethodCall(String input) {
		return input.matches("\\w+\\(.*\\);");
	}

    // Determine if the accumulated input forms a complete block
    public boolean isComplete(List<String> inputBuffer) {
        StringBuilder sb = new StringBuilder();
        for (String line : inputBuffer) {
            sb.append(line).append("\n");
        }
        String input = sb.toString();

        int openBraces = 0;
        for (char c : input.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') openBraces--;
        }

        // If braces are balanced, consider it complete
        return openBraces == 0;
    }

	// Helper method to parse arguments dynamically
	public Object parseArgument(String arg) {
		if ("null".equals(arg)) {
			return null;
		}
		// Parse basic types like Integer, Double, Boolean, etc.
		// Integer
		else if (arg.matches("-?\\d+")) {
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
	public Class<?> convertToPrimitive(Class<?> clazz) {
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

	public Class<?>[] extractParameterTypes(String paramString) {
		// No parameters
		if (paramString.isEmpty()) {
			return new Class<?>[0];
		}
		
		// Split by comma and space
		String[] paramTypes = Arrays.stream(paramString.split(", "))
			.map(param -> param.split(" ")[0].trim())
			.toArray(String[]::new);

		Class<?>[] classes = new Class<?>[paramTypes.length];
		
		for (int i = 0; i < paramTypes.length; i++) {
			String type = paramTypes[i].trim();
			// Map to corresponding Class
			classes[i] = mapToClass(type);
		}
		
		return classes;
	}

	public Class<?> mapToClass(String type) {
		switch (type) {
			case "int":
				return int.class;
			case "double":
				return double.class;
			case "boolean":
				return boolean.class;
			case "char":
				return char.class;
			case "byte":
				return byte.class;
			case "short":
				return short.class;
			case "long":
				return long.class;
			case "float":
				return float.class;
			default:
				try {
					// Try looking up the class by name
					return Class.forName(type);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Unknown parameter type: " + type);
				}
		}
	}
}
