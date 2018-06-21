/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

public final class SearchTargetPredicateRootContext<CTX, C> extends SearchPredicateContainerContextImpl<SearchPredicate, CTX, C> {

	public SearchTargetPredicateRootContext(SearchPredicateFactory<CTX, C> factory) {
		super( factory, new BuildingRootSearchPredicateDslContextImpl<>( factory ) );
	}

}
