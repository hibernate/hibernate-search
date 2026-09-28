package org.hibernate.search.engine.backend.types;

/**
 * Whether a field can be used in sorts.
 *
 * @author Emmanuel Bernard
 */
public enum Sortable {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * The field is not sortable.
	 */
	NO,
	/**
	 * The field is sortable
	 */
	YES
}
