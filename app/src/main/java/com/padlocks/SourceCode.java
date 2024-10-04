package com.padlocks;

public class SourceCode {
	private StringBuilder code;
	private boolean readyToExecute;

	public SourceCode() {
		code = new StringBuilder();
		readyToExecute = false;
	}

	public StringBuilder getCode() {
		return code;
	}

	public void setCode(StringBuilder code) {
		this.code = code;
	}

	public boolean isReadyToExecute() {
		return readyToExecute;
	}

	public void setReadyToExecute(boolean readyToExecute) {
		this.readyToExecute = readyToExecute;
	}
}