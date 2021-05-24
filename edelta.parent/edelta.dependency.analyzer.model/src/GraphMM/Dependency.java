/**
 */
package GraphMM;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dependency</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link GraphMM.Dependency#getSrc <em>Src</em>}</li>
 *   <li>{@link GraphMM.Dependency#getTrg <em>Trg</em>}</li>
 *   <li>{@link GraphMM.Dependency#isBidirectional <em>Bidirectional</em>}</li>
 * </ul>
 *
 * @see GraphMM.GraphMMPackage#getDependency()
 * @model
 * @generated
 */
public interface Dependency extends Edge {
	/**
	 * Returns the value of the '<em><b>Src</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Src</em>' reference.
	 * @see #setSrc(Node)
	 * @see GraphMM.GraphMMPackage#getDependency_Src()
	 * @model required="true"
	 * @generated
	 */
	Node getSrc();

	/**
	 * Sets the value of the '{@link GraphMM.Dependency#getSrc <em>Src</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Src</em>' reference.
	 * @see #getSrc()
	 * @generated
	 */
	void setSrc(Node value);

	/**
	 * Returns the value of the '<em><b>Trg</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Trg</em>' reference.
	 * @see #setTrg(Node)
	 * @see GraphMM.GraphMMPackage#getDependency_Trg()
	 * @model required="true"
	 * @generated
	 */
	Node getTrg();

	/**
	 * Sets the value of the '{@link GraphMM.Dependency#getTrg <em>Trg</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Trg</em>' reference.
	 * @see #getTrg()
	 * @generated
	 */
	void setTrg(Node value);

	/**
	 * Returns the value of the '<em><b>Bidirectional</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Bidirectional</em>' attribute.
	 * @see #setBidirectional(boolean)
	 * @see GraphMM.GraphMMPackage#getDependency_Bidirectional()
	 * @model
	 * @generated
	 */
	boolean isBidirectional();

	/**
	 * Sets the value of the '{@link GraphMM.Dependency#isBidirectional <em>Bidirectional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Bidirectional</em>' attribute.
	 * @see #isBidirectional()
	 * @generated
	 */
	void setBidirectional(boolean value);

} // Dependency
