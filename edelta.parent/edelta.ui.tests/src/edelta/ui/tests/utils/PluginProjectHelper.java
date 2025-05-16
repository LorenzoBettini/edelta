package edelta.ui.tests.utils;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.join;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.size;
import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;

import edelta.testutils.EdeltaTestUtils;

/**
 * Utility class for creating a Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 */
public class PluginProjectHelper {

	public static void assertNoErrors() throws CoreException {
		final Iterable<IMarker> markers = getErrorMarkers();
		assertEquals(
			("unexpected errors:\n" + 
				join(map(markers, it -> {
					try {
						return it.getAttribute(IMarker.LOCATION) + ", " + it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				}), "\n")), 0, size(markers));
	}

	public static void assertErrors(final CharSequence expected) throws CoreException {
		final Iterable<IMarker> markers = getErrorMarkers();
		assertEqualsStrings(expected,
				join(map(markers, it -> {
					try {
						return it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				}), "\n"));
	}

	public static Iterable<IMarker> getErrorMarkers() throws CoreException {
		return Stream.of(IResourcesSetupUtil.root().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE))
			.filter(it -> it.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
			.toList();
	}

	protected static void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
		assertEquals(
			EdeltaTestUtils.removeCR(expected.toString()),
			actual.toString());
	}

}
