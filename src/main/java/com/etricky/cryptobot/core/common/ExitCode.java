package com.etricky.cryptobot.core.common;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class ExitCode implements ExitCodeGenerator {

	int exitCode = -1;

	@Override
	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

}
