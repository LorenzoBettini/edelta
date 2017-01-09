/**
 * 
 */
package edelta.ui.wizard;

import java.util.List;

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
}
