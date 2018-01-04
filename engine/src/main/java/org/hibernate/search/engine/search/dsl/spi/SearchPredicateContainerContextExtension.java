/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.spi;


import java.util.Optional;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;

/**
 * @author Yoann Rodiere
 */
public interface SearchPredicateContainerContextExtension<N, T> {

	<C> T extendOrFail(SearchPredicateContainerContext<N> original,
			SearchTargetContext<C> targetContext, SearchDslContext<N, C> dslContext);

	<C> Optional<T> extendOptional(SearchPredicateContainerContext<N> original,
			SearchTargetContext<C> targetContext, SearchDslContext<N, C> dslContext);

}
