package edelta.interpreter;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EPackage;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEPackageManager;

/**
 * Used by the {@link EdeltaInterpreter} to return {@link EPackage} instances.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterEdeltaImpl extends AbstractEdelta {

	/**
	 * Uses the passed {@link EPackage}s to create an {@link EdeltaEPackageManager};
	 * if an {@link EPackage} appears several times in the list, only the first
	 * occurrence will be taken into consideration.
	 * 
	 * @param copiedEPackages
	 */
	public EdeltaInterpreterEdeltaImpl(Collection<EPackage> copiedEPackages) {
		super(new EdeltaEPackageManager() {
			private Map<String, EPackage> packageMap = copiedEPackages.stream().collect(
					Collectors.toMap(
							EPackage::getName,
							Function.identity(),
							(existingValue, newValue) -> existingValue));

			@Override
			public EPackage getEPackage(String packageName) {
				return packageMap.get(packageName);
			}
		});
	}

}
