/**
 * 
 */
package edelta.ui.outline.actions;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_MODIFY_ECORE_OPERATION;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EPACKAGE;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;

import edelta.ui.outline.EdeltaOutlineTreeProvider.EcoreOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.EcorePropertyOutlineNode;

/**
 * From an {@link XExpression} responsible of the modification of an {@link ENamedElement}
 * in the editor, navigate to the corresponding element in the outline.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaOutlineWithEditorLinker extends OutlineWithEditorLinker {

	@Override
	protected IOutlineNode findBestNode(IOutlineNode input, ITextRegion selectedTextRegion) {
		final var findBestNode = super.findBestNode(input, selectedTextRegion);
		if (findBestNode instanceof EObjectNode eObjectNode && eObjectNode.getEClass() == EDELTA_MODIFY_ECORE_OPERATION) {
			/* since XExpressions are not shown in the outline, when we select
			 * such an expression, by default the containing modifyEcore node is
			 * selected. We try and find a node representing an Ecore element
			 * that is modified by the selected XExpression */
			var ePackages = eObjectNode.getParent().getChildren().stream()
				.filter(node -> EPACKAGE == ((EObjectNode) node).getEClass())
				.toList();
			var property = ePackages.stream()
				.map(node -> findPropertyNode(node, selectedTextRegion))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
			var element = ePackages.stream()
				.map(node -> findENamedElementNode(node, selectedTextRegion))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
			var exact = property != null ? property : element;
			if (selectedTextRegion.getLength() == 0) {
				var line = lineAt(selectedTextRegion.getOffset());
				if (exact != null && startsOnLine(exact.getSignificantTextRegion(), line)) {
					return exact;
				}
				var nearest = ePackages.stream()
					.flatMap(this::descendants)
					.filter(this::isResponsibleNode)
					.filter(this::hasSourceRegion)
					.filter(node -> startsOnLine(node.getSignificantTextRegion(), line))
					.min(Comparator
						.comparingInt((IOutlineNode node) ->
							distance(node.getSignificantTextRegion(), selectedTextRegion.getOffset()))
						.thenComparingInt(node -> node instanceof EcoreOutlineNode ? 0 : 1))
					.orElse(null);
				if (nearest != null) {
					return nearest;
				}
			}
			return exact != null ? exact : findBestNode;
		}
		return findBestNode;
	}

	private IOutlineNode findPropertyNode(IOutlineNode node, ITextRegion selectedTextRegion) {
		for (var child : node.getChildren()) {
			if (child instanceof EcorePropertyOutlineNode property && property.isDirectChange() &&
					contains(property.getSignificantTextRegion(), selectedTextRegion)) {
				return property;
			}
			if (child instanceof EcoreOutlineNode) {
				var match = findPropertyNode(child, selectedTextRegion);
				if (match != null) {
					return match;
				}
			}
		}
		return null;
	}

	private IOutlineNode findENamedElementNode(IOutlineNode node, ITextRegion selectedTextRegion) {
		for (var child : node.getChildren()) {
			if (!(child instanceof EcoreOutlineNode ecoreNode)) {
				continue;
			}
			// our Outline nodes for Ecore elements are already associated with the
			// text region of the corresponding responsible XExpression
			final var recursiveFind = findENamedElementNode(child, selectedTextRegion);
			// first check whether there's a child that matches, since,
			// as said before, an outer expression region might match an inner one
			if (recursiveFind != null) {
				return recursiveFind;
			} else if (contains(ecoreNode.getSignificantTextRegion(), selectedTextRegion)) {
				return ecoreNode;
			}
		}
		return null;
	}

	private boolean contains(ITextRegion region, ITextRegion selection) {
		return region != null && region.contains(selection);
	}

	/** Includes nested metamodel elements and their property rows. */
	private Stream<IOutlineNode> descendants(IOutlineNode node) {
		return Stream.concat(Stream.of(node), node.getChildren().stream().flatMap(this::descendants));
	}

	private boolean isResponsibleNode(IOutlineNode node) {
		return node instanceof EcoreOutlineNode ||
			(node instanceof EcorePropertyOutlineNode property && property.isDirectChange());
	}

	private boolean hasSourceRegion(IOutlineNode node) {
		return node.getSignificantTextRegion() != null;
	}

	/** Returns an empty range if the outline and editor documents are temporarily out of sync. */
	private IRegion lineAt(int offset) {
		try {
			return textViewer.getDocument().getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			return new Region(0, 0);
		}
	}

	private boolean startsOnLine(ITextRegion region, IRegion line) {
		return region.getLength() > 0 && region.getOffset() >= line.getOffset() &&
			region.getOffset() < line.getOffset() + line.getLength();
	}

	private int distance(ITextRegion region, int offset) {
		return Math.max(0, region.getOffset() - offset) +
			Math.max(0, offset - region.getEndOffset());
	}
}
