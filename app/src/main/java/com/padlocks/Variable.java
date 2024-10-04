package com.padlocks;

public class Variable {
	public enum AccessLevel {
		PUBLIC, PRIVATE, PROTECTED, DEFAULT
	}

	private AccessLevel access;
	private boolean isStatic;
	private String name;
	private String type;
	private Object value;
	private String input;

	public Variable(AccessLevel access, boolean isStatic, String name, String type, Object value, String input) {
		this.access = access;
		this.isStatic = isStatic;
		this.name = name;
		this.type = type;
		this.value = value;
		this.input = input;
	}

	public AccessLevel getAccess() {
		return access;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
		this.input = this.input.replaceFirst("=.*", "= " + value) + ";";
	}

	public String getInput() {
		return input;
	}

	@Override
	public String toString() {
		return this.getInput();
	}

	public static AccessLevel getAccessLevel(String input) {
		if (input.startsWith("public")) {
			return AccessLevel.PUBLIC;
		} else if (input.startsWith("private")) {
			return AccessLevel.PRIVATE;
		} else if (input.startsWith("protected")) {
			return AccessLevel.PROTECTED;
		} else {
			return AccessLevel.DEFAULT;
		}
	}
}