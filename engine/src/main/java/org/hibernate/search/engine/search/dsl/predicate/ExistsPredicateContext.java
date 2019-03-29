/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define an "exists" predicate.
 */
public interface ExistsPredicateContext {

	/**
	 * Target the given field in the "exists" predicate.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A context allowing to set further options or get the resulting predicate.
	 */
	ExistsPredicateTerminalContext onField(String absoluteFieldPath);

}
