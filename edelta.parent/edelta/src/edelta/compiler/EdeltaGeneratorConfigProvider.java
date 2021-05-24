package edelta.compiler;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.GeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.XbaseCompiler;

/**
 * In order to make {@link XbaseCompiler} generate Java 8 lambdas also when
 * running from xtext-maven-plugin.
 * 
 * @see https://github.com/LorenzoBettini/edelta/issues/60
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaGeneratorConfigProvider extends GeneratorConfigProvider {

	@Override
	public GeneratorConfig get(EObject context) {
		final var generatorConfig = super.get(context);
		generatorConfig.setJavaSourceVersion(JavaVersion.JAVA11);
		return generatorConfig;
	}
}
