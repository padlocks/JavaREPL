package com.padlocks;

import java.io.File;

import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class SimpleJavaREPL {

  public static void main(String[] args) throws Exception {
    Terminal terminal = TerminalBuilder.builder().system(true).build();
    Completer completer = new StringsCompleter(Keywords.get());
    Highlighter highlighter = new JavaSyntaxHighlighter();
    LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(completer)
        .highlighter(highlighter)
        .build();
    String input;

    System.out.println("Welcome to the Simple Java REPL. Type 'exit' to quit. Use tab while typing for autocomplete.");

    // Execution Loop
    while (true) {
      // Prompt the user for input
      input = reader.readLine(">> ").trim();

      // Check if the user wants to exit the REPL
      if (input.equalsIgnoreCase("exit")) {
        System.out.println("Exiting REPL...");

        // Delete ./tmp directory
        File dir = new File("./tmp/");
        if (dir.exists()) {
          File[] files = dir.listFiles();
          if (files != null) {
            for (File file : files) {
              file.delete();
            }
          }
          dir.delete();
        }
        // Exit the REPL
        break;
      }

      // Check if the input is a block of code
      if (input.endsWith("{")) {
        StringBuilder codeBlock = new StringBuilder(input);
        int openBraces = 1;
        while (openBraces > 0) {
          String prompt = "... ".repeat(openBraces);
          input = reader.readLine(prompt).trim();
          codeBlock.append("\n").append(input);
          openBraces += countOccurrences(input, '{');
          openBraces -= countOccurrences(input, '}');
        }
        input = codeBlock.toString();
      }

      try {
        // Evaluate the input expression, statement or code block
        Evaluator.evaluateInput(input);
      } catch (Exception e) {
        // System.err.println("Error: " + e.getMessage());
        // e.printStackTrace();
      }
    }
  }

  // Count occurrences of a character in a string
  private static int countOccurrences(String str, char ch) {
    int count = 0;
    for (char c : str.toCharArray()) {
      if (c == ch) {
        count++;
      }
    }
    return count;
  }
}
