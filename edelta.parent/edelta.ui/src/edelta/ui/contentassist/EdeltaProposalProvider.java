/*
 * generated by Xtext 2.21.0
 */
package edelta.ui.contentassist;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaMigration;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaPackage;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaEcoreHelper;
import edelta.util.EdeltaModelUtil;

/**
 * See
 * https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#content-assist
 * on how to customize the content assistant.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaProposalProvider extends AbstractEdeltaProposalProvider {

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private EdeltaEcoreHelper ecoreHelper;

	@Inject
	private IResourceScopeCache cache;

	protected class EdeltaProposalCreator extends XbaseProposalCreator {

		public EdeltaProposalCreator(ContentAssistContext contentAssistContext, String ruleName,
				IQualifiedNameConverter qualifiedNameConverter) {
			super(contentAssistContext, ruleName, qualifiedNameConverter);
		}

		/**
		 * If we have an ambiguous reference, then set the replacement string to the
		 * fully qualified name, so that the resulting program will not contain an
		 * ambiguous reference. This relies on the
		 * {@link EdeltaContentAssistEObjectDescription} created by
		 * {@link EdeltaProposalProvider#completeEdeltaEcoreDirectReference_Enamedelement}.
		 */
		@Override
		public ICompletionProposal apply(IEObjectDescription candidate) {
			final var completionProposal = super.apply(candidate);
			if (completionProposal == null)
				return completionProposal;
			if (candidate instanceof EdeltaContentAssistEObjectDescription desc && desc.isAmbiguous()) {
				final var configurableProposal = (ConfigurableCompletionProposal) completionProposal;
				final var originalReplacement = configurableProposal.getReplacementString();
				final var qualifiedReplacement = desc.getQualifiedName().toString();
				configurableProposal.setReplacementString(qualifiedReplacement);
				// the cursor position after applying the proposal must be updated as well
				// to the length of the new replacement string
				configurableProposal.setCursorPosition(qualifiedReplacement.length());
				final var originalMatcher = configurableProposal.getMatcher();
				// the prefix matcher must be updated so that it takes into
				// consideration the original replacement string, i.e., the one
				// not fully qualified, otherwise the filtering won't work after
				// the proposals have been shown and the user keeps on typing.
				configurableProposal.setMatcher(
					new EdeltaOverriddenPrefixMatcher(originalMatcher, originalReplacement));
			}
			return completionProposal;
		}
	}

	/**
	 * Avoids proposing subpackages since in Edelta they are not allowed
	 * to be directly imported.
	 */
	@Override
	public void completeEdeltaProgram_EPackages(EObject model, Assignment assignment, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		lookupCrossReference(
			((CrossReference) assignment.getTerminal()),
			context,
			acceptor,
			// EPackage are not loaded at this point, so we cannot rely
			// on super package relation.
			// Instead we rely on the fact that subpackages have segments
			(IEObjectDescription desc) ->
				desc.getQualifiedName().getSegmentCount() == 1
		);
	}

	/**
	 * Only proposes elements that are available in this context, and also
	 * store information about possible ambiguities using {@link EdeltaContentAssistEObjectDescription},
	 * used then by {@link EdeltaProposalCreator#apply(IEObjectDescription)}
	 */
	@Override
	public void completeEdeltaEcoreDirectReference_Enamedelement(EObject model,
			Assignment assignment, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		EdeltaAccessibleElements accessibleElements;
		if (notInsideModifyEcore(model)) {
			accessibleElements = 
				cache.get("getOriginalMetamodelsAccessibleElements", model.eResource(),
					() -> ecoreHelper.fromEPackagesToAccessibleElements(
						EdeltaModelUtil.getMetamodels(model)));
		} else {
			accessibleElements = getAccessibleElements(model);
		}
		final var countByName = accessibleElements.stream()
			.collect(groupingBy(e -> e.getElement().getName(),
						counting()));
		createENamedElementProposals(model, context,
			acceptor,
			new SimpleScope(
				Iterables.transform(accessibleElements,
					e -> {
						final var name = e.getElement().getName();
						// we also want to show the fully qualified name
						return new EdeltaContentAssistEObjectDescription(
							QualifiedName.create(name),
							e.getQualifiedName(),
							e.getElement(),
							// and store whether a proposal is ambiguous
							countByName.get(name) > 1);
						}
					)
				)
			);
	}

	/**
	 * Only proposes children that are available in this context.
	 */
	@Override
	public void completeEdeltaEcoreReference_Enamedelement(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		if (notInsideModifyEcore(model)) {
			super.completeEdeltaEcoreReference_Enamedelement(
					model, assignment, context, acceptor);
			return;
		}
		final var accessibleElements = getAccessibleElements(model);
		final var qualification = ((EdeltaEcoreQualifiedReference) model)
			.getQualification();
		String qualificationText = EdeltaModelUtil.getEcoreReferenceText(qualification);
		accessibleElements.stream()
			.filter(e -> e.getQualifiedName().toString().endsWith(qualificationText))
			.findFirst()
			.ifPresent(e -> 
				createENamedElementProposals(model, context, acceptor,
					Scopes.scopeFor(
						ecoreHelper.getENamedElements(e.getElement()))));
	}

	private boolean notInsideModifyEcore(EObject model) {
		return getContainerOfType(model, EdeltaModifyEcoreOperation.class) == null;
	}

	private EdeltaAccessibleElements getAccessibleElements(EObject model) {
		return derivedStateHelper.getAccessibleElements(
			getContainerOfType(model, EdeltaEcoreReferenceExpression.class));
	}

	private void createENamedElementProposals(EObject model, ContentAssistContext context, ICompletionProposalAcceptor acceptor,
			IScope scope) {
		getCrossReferenceProposalCreator()
			.lookupCrossReference(
				scope,
				model,
				EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE__ENAMEDELEMENT,
				acceptor,
				Predicates.<IEObjectDescription> alwaysTrue(),
				getProposalFactory("ID", context));
	}

	@Override
	protected Function<IEObjectDescription, ICompletionProposal> getProposalFactory(final String ruleName,
			final ContentAssistContext contentAssistContext) {
		return new EdeltaProposalCreator(contentAssistContext, ruleName, getQualifiedNameConverter());
	}

	/**
	 * Proposes as 'to' nsURI the same as the original one: it will be invalid,
	 * but it's a good starting point since it's enough to change the version.
	 */
	@Override
	public void completeEdeltaMigration_To(EObject model, Assignment assignment, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		acceptor.accept(createCompletionProposal(
			"\"" + "" + ((EdeltaMigration) model).getNsURI().getNsURI() + "\"", context));
	}
}
