package com.padlocks;

public class Variable {
	private String name;
	private String type;
	private Object value;
	private String input;

	public Variable(String name, String type, Object value, String input) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.input = input;
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
	}

	public String getInput() {
		return input;
	}

	@Override
	public String toString() {
		return this.getInput();
	}
}