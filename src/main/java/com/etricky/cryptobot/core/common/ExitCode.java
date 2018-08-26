package com.etricky.cryptobot.core.common;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class ExitCode implements ExitCodeGenerator {

	public static final int EXIT_CODE_ERROR = -1;
	private int exitCode = EXIT_CODE_ERROR;

	@Override
	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

}
