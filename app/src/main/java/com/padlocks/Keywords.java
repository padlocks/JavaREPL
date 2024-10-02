package com.padlocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.utils.AttributedStyle;

class Keywords {
  private static final List<String> dataTypes = Arrays.asList(
	"int", "double", "float", "char", "byte", "short", "long", "boolean", "void"
  );
  private static final List<String> literals = Arrays.asList(
	"true", "false", "null"
  );
  private static final List<String> operators = Arrays.asList(
	"+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "=", 
	"+=", "-=", "*=", "/=", "&", "|", "^", "~", "<<", ">>", ">>>", "++", "--", "?:"
  );
  private static final List<String> comments = Arrays.asList(
	"//", "/*", "*/", "/**", "*/"
  );
  private static final List<String> annotations = Arrays.asList(
	"@Override", "@Deprecated", "@SuppressWarnings"
  );
  private static final List<String> brackets = Arrays.asList(
	"{", "}", "(", ")", "[", "]"
  );
  private static final List<String> exceptions = Arrays.asList(
	"Exception"
  );
  private static final List<String> modifiers = Arrays.asList(
	"public", "private", "protected", "final", "static", "abstract", "synchronized", 
	"native", "class", "interface", "extends", "implements", "package", "import", "super", 
	"this", "new", "instanceof", "const", "volatile", "transient", "strictfp"
  );
  private static final List<String> controlStatements = Arrays.asList(
	"if", "else", "switch", "case", "for", "while", "do", "break", "continue", "return", 
	"try", "catch", "finally", "throw", "throws", "assert"
  );

	private static final List<String> keywords = new ArrayList<>();

	static {
		keywords.addAll(dataTypes);
		keywords.addAll(literals);
		keywords.addAll(annotations);
		keywords.addAll(exceptions);
		keywords.addAll(modifiers);
		keywords.addAll(controlStatements);
	}

	// Styles
	static final AttributedStyle dataTypeStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
	static final AttributedStyle literalStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
	static final AttributedStyle operatorStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
	static final AttributedStyle commentStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
	static final AttributedStyle annotationStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
	static final AttributedStyle bracketStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
	static final AttributedStyle exceptionStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
	static final AttributedStyle modifierStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
	static final AttributedStyle controlStatementStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

  static List<String> get() {
	return keywords;
  }

  static boolean isDataType(String token) {
	return dataTypes.contains(token);
  }

  static boolean isLiteral(String token) {
	return literals.contains(token);
  }

  static boolean isOperator(String token) {
	return operators.contains(token);
  }

  static boolean isComment(String token) {
	// Check for single-line comments
	if (token.startsWith("//")) {
	  return true;
	}
    // Check for multi-line comments
	return token.startsWith("/*") || token.endsWith("*/");
  }

  static boolean isAnnotation(String token) {
	// Check if the token starts with @
	return token.startsWith("@");
  }

  static boolean isBracket(String token) {
	return brackets.contains(token);
  }

  static boolean isException(String token) {
	// Check if token ends with "Exception"
	return token.endsWith("Exception");
  }

  static boolean isModifier(String token) {
	return modifiers.contains(token);
  }

  static boolean isControlStatement(String token) {
	return controlStatements.contains(token);
  }

  static boolean isKeyword(String token) {
	return keywords.contains(token);
  }
}