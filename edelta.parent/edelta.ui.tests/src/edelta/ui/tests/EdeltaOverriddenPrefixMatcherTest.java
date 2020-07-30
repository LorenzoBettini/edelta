package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.xtext.ui.editor.contentassist.PrefixMatcher.IgnoreCase;
import org.junit.Test;

import edelta.ui.contentassist.EdeltaOverriddenPrefixMatcher;

public class EdeltaOverriddenPrefixMatcherTest {

	@Test
	public void testEdeltaOverriddenPrefixMatcher() {
		EdeltaOverriddenPrefixMatcher matcher =
			new EdeltaOverriddenPrefixMatcher(new IgnoreCase(), "testString");
		// verify that the first parameter is actually ignored
		// and the string passed to the constructor is considered instead
		assertThat(matcher.isCandidateMatchingPrefix("unrelated", "test"))
			.isTrue();
		assertThat(matcher.isCandidateMatchingPrefix("testString", "anotherPrefix"))
			.isFalse();
	}

}
