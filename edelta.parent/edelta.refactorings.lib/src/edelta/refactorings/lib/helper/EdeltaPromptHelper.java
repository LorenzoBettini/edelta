package edelta.refactorings.lib.helper;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Utilities for prompting the user during migration.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaPromptHelper {

	private static Scanner scanner;

	private EdeltaPromptHelper() {
		// Only static methods
	}

	private static void ensureScannerIsSet() {
		if (scanner == null)
			scanner = new Scanner(System.in);
	}

	/**
	 * Delegates to {@link System#out}'s {@link PrintStream#println(String)}
	 * 
	 * @param message
	 */
	public static void show(String message) {
		System.out.println(message); // NOSONAR
	}

	/**
	 * Presents the choices that can be selected by their numbers, make sure the
	 * selected number is valid and return the corresponding String.
	 * 
	 * @param choices
	 * @return
	 */
	public static String choice(List<String> choices) {
		var i = 0;
		for (String choice : choices) {
			show("  " + ++i + " " + choice);
		}
		ensureScannerIsSet();
		while (true) {
			showNoNl("Choice? ");
			var chosen = scanner.nextLine();
			try {
				int selectedNum = Integer.parseInt(chosen);
				if (selectedNum == 0 || selectedNum > choices.size())
					showError("Not a valid choice: " + chosen);
				else if (selectedNum < 0)
					break;
				else
					return choices.get(selectedNum - 1);
			} catch (NumberFormatException e) {
				showError("Not a valid number: " + chosen);
			}
		}
		return null;
	}

	/**
	 * Delegates to {@link System#err}'s {@link PrintStream#println(String)}
	 * 
	 * @param message
	 */
	public static void showError(String message) {
		System.err.println(message); // NOSONAR
	}

	/**
	 * Delegates to {@link System#out}'s {@link PrintStream#print(String)}
	 * 
	 * @param message
	 */
	public static void showNoNl(String message) {
		System.out.print(message); // NOSONAR
	}

	/**
	 * Must be called only after we are sure that we don't need the scanner anymore.
	 * Typically used in tests.
	 */
	public static void close() {
		if (scanner != null)
			scanner.close();
		scanner = null;
	}

}
