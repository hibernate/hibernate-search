/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether the field can be used in projections.
 * <p>
 * This usually means that the field will have doc-values stored in the index.
 */
public enum Aggregable {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * Do not allow aggregation on the field.
	 */
	NO,
	/**
	 * Allow aggregation on the field.
	 */
	YES
}
