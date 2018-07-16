package com.etricky.cryptobot.core.interfaces.shell;

import java.util.List;
import java.util.stream.Collectors;

import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.jline.JLineShellAutoConfiguration;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ShellInputParser implements InputProvider {

	@Setter
	@NonNull
	private String command;

	/**
	 * Based on readInput in
	 * {@link org.springframework.shell.jline.FileInputProvider}
	 * 
	 * @param command
	 * @return
	 */
	@Override
	public Input readInput() {
		Parser parser = new JLineShellAutoConfiguration().parser();
		StringBuilder sb = new StringBuilder();
		Input returnInupt;

		if (command == null) {
			returnInupt = null;
		} else {
			sb.append(command);
			ParsedLine parsedLine = parser.parse(sb.toString(), sb.toString().length());
			returnInupt = new ParsedLineInput(parsedLine);
		}

		// in a terminal, after processing the command, the shell waits for the next
		// command. In this case, command must be set to null to stop the processing of
		// the shell
		command = null;
		return returnInupt;
	}

	/**
	 * Blunt copy of {@link org.springframework.shell.jline.ParsedLineInput}
	 * 
	 * @author rgoncalves
	 * 
	 */
	class ParsedLineInput implements Input {

		private final ParsedLine parsedLine;

		ParsedLineInput(ParsedLine parsedLine) {
			this.parsedLine = parsedLine;
		}

		@Override
		public String rawText() {
			return parsedLine.line();
		}

		@Override
		public List<String> words() {
			return parsedLine.words().stream().map(s -> s.replaceAll("^\\n+|\\n+$", "")) // CR at beginning/end of line introduced by backslash
																							// continuation
					.map(s -> s.replaceAll("\\n+", " ")) // CR in middle of word introduced by return inside a quoted string
					.collect(Collectors.toList());
		}
	}
}
