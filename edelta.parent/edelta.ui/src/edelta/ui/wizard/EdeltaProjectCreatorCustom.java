/**
 * 
 */
package edelta.ui.wizard;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Lorenzo Bettini
 *
 */
public class EdeltaProjectCreatorCustom extends EdeltaProjectCreator {

	@Override
	protected List<String> getRequiredBundles() {
		return Lists.newArrayList(EdeltaProjectCreator.DSL_PROJECT_NAME+".lib");
	}

	@Override
	protected List<String> getAllFolders() {
		List<String> allFolders = super.getAllFolders();
		allFolders = Lists.newArrayList(allFolders);
		allFolders.add("model");
		return ImmutableList.copyOf(allFolders);
	}
}
