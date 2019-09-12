/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step in an "exists" predicate definition, where the target field can be set.
 */
public interface ExistsPredicateFieldStep {

	/**
	 * Target the given field in the "exists" predicate.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 */
	ExistsPredicateOptionsStep field(String absoluteFieldPath);

	/**
	 * @deprecated Use {@link #field(String)} instead.
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 */
	@Deprecated
	default ExistsPredicateOptionsStep onField(String absoluteFieldPath) {
		return field( absoluteFieldPath );
	}

}
