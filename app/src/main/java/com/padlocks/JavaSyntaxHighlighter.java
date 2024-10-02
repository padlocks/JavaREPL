package com.padlocks;

import org.jline.reader.LineReader;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

class JavaSyntaxHighlighter extends DefaultHighlighter {
	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		AttributedStringBuilder builder = new AttributedStringBuilder();
		String[] tokens = buffer.split("(?<=\\s)|(?=\\s)");
		boolean inComment = false;

		for (String token : tokens) {
			// Check if the token is part of a comment
			// TODO: Fix multiline comments
			if (inComment) {
				builder.append(token, AttributedStyle.DEFAULT);
				if (token.contains("*/")) {
					inComment = false;
				}
			} else if (token.trim().startsWith("//")) {
				builder.append(token, AttributedStyle.DEFAULT);
				inComment = true;
				if (token.contains("\n")) {
					inComment = false;
				}
			} else if (token.trim().startsWith("/*")) {
				builder.append(token, AttributedStyle.DEFAULT);
				inComment = true;
				if (token.contains("*/")) {
					inComment = false;
				}
			// Check if the token is a keyword, apply the appropriate style
			} else if (token.trim().isEmpty()) {
				builder.append(token, AttributedStyle.DEFAULT);
			} else if (Keywords.isDataType(token)) {
				builder.append(token, Keywords.dataTypeStyle);
			} else if (Keywords.isOperator(token)) {
				builder.append(token, Keywords.operatorStyle);
			} else if (Keywords.isAnnotation(token)) {
				builder.append(token, Keywords.annotationStyle);
			} else if (Keywords.isException(token)) {
				builder.append(token, Keywords.exceptionStyle);
			} else if (Keywords.isModifier(token)) {
				builder.append(token, Keywords.modifierStyle);
			} else if (Keywords.isControlStatement(token)) {
				builder.append(token, Keywords.controlStatementStyle);
			} else {
				builder.append(token, AttributedStyle.DEFAULT);
			}
		}
		return builder.toAttributedString();
	}
}