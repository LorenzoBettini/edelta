package edelta.interpreter;

import java.util.List;
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

	public EdeltaInterpreterEdeltaImpl(List<EPackage> ePackages) {
		super(new EdeltaEPackageManager() {
			private Map<String, EPackage> packageMap = ePackages.stream().collect(
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
