/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.dsl.spi.SearchPredicateContributor;

public interface SearchPredicateBuilder<C> extends SearchPredicateContributor<C> {

	void boost(float boost);

	/**
	 * Contribute exactly one predicate to the collector (no more, no less).
	 * @param collector A collector to contribute to.
	 */
	@Override
	void contribute(C collector);
}
