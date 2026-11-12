package edelta.standalone;

import java.util.List;

/** Reports validation errors found while compiling Edelta sources. */
public class EdeltaCompilationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final List<String> diagnostics;

	public EdeltaCompilationException(final List<String> diagnostics) {
		super(String.join(System.lineSeparator(), diagnostics));
		this.diagnostics = List.copyOf(diagnostics);
	}

	/** Returns file and line diagnostics for all failed resources. */
	public List<String> getDiagnostics() {
		return diagnostics;
	}
}
