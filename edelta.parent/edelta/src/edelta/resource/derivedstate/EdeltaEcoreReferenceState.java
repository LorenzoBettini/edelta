package edelta.resource.derivedstate;

import org.eclipse.emf.ecore.ENamedElement;

import edelta.edelta.EdeltaEcoreReference;

/**
 * Additional information on {@link EdeltaEcoreReference}
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreReferenceState {

	public static class EdeltaEcoreReferenceStateInformation {
		private String type;
		private String ePackageName;
		private String eClassifierName;
		private String eNamedElementName;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getEPackageName() {
			return ePackageName;
		}

		public void setEPackageName(String ePackageName) {
			this.ePackageName = ePackageName;
		}

		public String getEClassifierName() {
			return eClassifierName;
		}

		public void setEClassifierName(String eClassifierName) {
			this.eClassifierName = eClassifierName;
		}

		public String getENamedElementName() {
			return eNamedElementName;
		}

		public void setENamedElementName(String eNamedElementName) {
			this.eNamedElementName = eNamedElementName;
		}

	}

	private ENamedElement originalEnamedelement;
	private EdeltaEcoreReferenceStateInformation information;

	public ENamedElement getOriginalEnamedelement() {
		return originalEnamedelement;
	}

	public void setOriginalEnamedelement(ENamedElement originalEnamedelement) {
		this.originalEnamedelement = originalEnamedelement;
	}

	public EdeltaEcoreReferenceStateInformation getInformation() {
		return information;
	}

	public void setInformation(EdeltaEcoreReferenceStateInformation information) {
		this.information = information;
	}
}
