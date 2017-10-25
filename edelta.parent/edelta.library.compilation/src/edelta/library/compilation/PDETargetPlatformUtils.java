package edelta.library.compilation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Implements workaround suggested here:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=343156
 * This is required when running SwtBot tests in Tycho
 * that requires the PDE, for example, for testing that the
 * imported projects compile fine, or if they use the DSL, which
 * requires PDE projects dependencies.
 * 
 * @author Lorenzo Bettini - some adaptations
 */
public class PDETargetPlatformUtils {
	
	private static boolean targetPlatformAlreadySet = false;

	/**
	 * Sets a target platform in the test platform to get workspace builds OK
	 * with PDE.
	 * 
	 * @throws Exception
	 */
	public static void setTargetPlatform() throws Exception {
		if (System.getProperty("buildingWithTycho") != null) {
			if (targetPlatformAlreadySet) {
				System.out.println("Target platform already set");
				return;
			}
			targetPlatformAlreadySet = true;
			System.out.println("Generating a target platform");
		} else {
			System.out.println("Using the Workbench's target platform");
			return;
		}
		ITargetPlatformService tpService = TargetPlatformService.getDefault();
		ITargetDefinition targetDef = tpService.newTarget();
		Bundle currentBundle = FrameworkUtil.getBundle(PDETargetPlatformUtils.class);
		System.out.println("Current bundle: " + currentBundle);
		targetDef.setName("Tycho platform");
		Bundle[] bundles = Platform.getBundle("org.eclipse.core.runtime").getBundleContext().getBundles();
		List<ITargetLocation> bundleContainers = new ArrayList<ITargetLocation>();
		Set<File> dirs = new HashSet<File>();
		System.out.println("Bundles for the target platform:");
		for (Bundle bundle : bundles) {
			if (bundle.equals(currentBundle)) {
				// we skip the current bundle, otherwise the folder for the target platform
				// will include the absolute directory of the maven parent project
				// since the projects are nested in the parent project the result
				// would be that Java packages of our project will be available twice
				// and Java won't be able to find our classes leading in compilation
				// errors during our tests.
				System.err.println("*** Skipping current bundle: " + currentBundle);
				continue;
			}
//			AbstractBundle bundleImpl = (AbstractBundle) bundle;
//			BaseData bundleData = (BaseData) bundleImpl.getBundleData();
			EquinoxBundle bundleImpl = (EquinoxBundle) bundle;
			Generation generation = (Generation) bundleImpl.getModule().getCurrentRevision().getRevisionInfo();
			File file = generation.getBundleFile().getBaseFile();
			File folder = file.getParentFile();
			if (!dirs.contains(folder)) {
				dirs.add(folder);
				String absolutePath = folder.getAbsolutePath();
				System.out.println(bundle + " - " + absolutePath);
				bundleContainers.add(tpService.newDirectoryLocation(absolutePath));
			}
		}
		System.out.println("");
		System.out.println("Bundles added the target platform.");
		targetDef.setTargetLocations(bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
		targetDef.setArch(Platform.getOSArch());
		targetDef.setOS(Platform.getOS());
		targetDef.setWS(Platform.getWS());
		targetDef.setNL(Platform.getNL());
		// targetDef.setJREContainer()
		tpService.saveTargetDefinition(targetDef);

		System.out.print("Loading target platform... ");
		Job job = new LoadTargetDefinitionJob(targetDef);
		job.schedule();
		job.join();
		System.out.println("DONE.");
	}
}
