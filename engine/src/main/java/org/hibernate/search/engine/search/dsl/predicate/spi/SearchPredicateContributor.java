/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

/**
 * A search predicate contributor, i.e. an element of the DSL that will ultimately be asked
 * to produce a search predicate builder.
 *
 * @param <B> The implementation type of builders
 * This type is backend-specific. See {@link SearchPredicateBuilder#toImplementation()}
 */
public interface SearchPredicateContributor<B> {

	/**
	 * @return The contributed predicate builder.
	 */
	B contribute();

}
