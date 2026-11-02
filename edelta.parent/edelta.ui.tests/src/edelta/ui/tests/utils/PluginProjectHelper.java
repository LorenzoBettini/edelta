package edelta.ui.tests.utils;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
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
		final var markers = getErrorMarkers();
		assertEquals(
			"unexpected errors:\n" + markers.stream()
				.map(it -> {
					try {
						return it.getAttribute(IMarker.LOCATION) + ", " + it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				})
				.collect(Collectors.joining("\n")),
			0, markers.size());
	}

	public static void assertErrors(final CharSequence expected) throws CoreException {
		final var markers = getErrorMarkers();
		assertEqualsStrings(expected,
			markers.stream()
				.map(it -> {
					try {
						return it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				})
				.map(String::valueOf)
				.collect(Collectors.joining("\n")));
	}

	public static List<IMarker> getErrorMarkers() throws CoreException {
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
