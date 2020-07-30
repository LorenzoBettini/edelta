package edelta.ui.contentassist;

import org.eclipse.xtext.ui.editor.contentassist.PrefixMatcher;

/**
 * Used in our content assist when we change the replacement string with the
 * fully qualified string, so that the prefix is matched against the original
 * replacement string and not with the fully qualified one (which would break
 * the actual filtering when the user continues typing after the proposals are
 * shown).
 * 
 * @author Lorenzo Bettini
 *
 */
public final class EdeltaOverriddenPrefixMatcher extends PrefixMatcher {
	private final PrefixMatcher originalMatcher;
	private final String originalReplacement;

	public EdeltaOverriddenPrefixMatcher(PrefixMatcher originalMatcher, String originalReplacement) {
		this.originalMatcher = originalMatcher;
		this.originalReplacement = originalReplacement;
	}

	/**
	 * Ignores the passed name parameter and used the originalReplacement
	 */
	@Override
	public boolean isCandidateMatchingPrefix(String name, String prefix) {
		return originalMatcher.isCandidateMatchingPrefix(originalReplacement, prefix);
	}
}