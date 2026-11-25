package org.hibernate.search.engine.backend.types;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Whether a field value can be used in score functions (e.g. field_value_factor).
 */
@Incubating
public enum Scorable {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * The field value is not available for score functions.
	 */
	NO,
	/**
	 * The field value is available for score functions.
	 */
	YES
}
