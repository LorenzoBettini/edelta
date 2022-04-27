package edelta.refactorings.lib.helper;

import java.util.List;
import java.util.Scanner;

/**
 * Utilities for prompting the user during migration.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaPromptHelper {

	public void show(String message) {
		System.out.println(message); // NOSONAR
	}

	public String choice(List<String> choices) {
		var i = 0;
		for (String choice : choices) {
			show("  " + ++i + " " + choice);
		}
		showNoNl("Choice? ");
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				var chosen = scanner.next();
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
		}
		return null;
	}

	private void showError(String message) {
		System.err.println(message); // NOSONAR
	}

	private void showNoNl(String message) {
		System.out.print(message); // NOSONAR
	}

}
