/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl;

import java.util.function.Consumer;

/**
 * @author Yoann Rodiere
 */
public interface BooleanJunctionContext<N> extends QueryClauseContext<BooleanJunctionContext<N>>, ExplicitEndContext<N> {

	QueryClauseContainerContext<BooleanJunctionContext<N>> must();

	QueryClauseContainerContext<BooleanJunctionContext<N>> mustNot();

	QueryClauseContainerContext<BooleanJunctionContext<N>> should();

	QueryClauseContainerContext<BooleanJunctionContext<N>> filter();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code
	 * and removing the need to call .end() on nested clause contexts.
	 */

	default BooleanJunctionContext<N> must(Consumer<QueryClauseContainerContext<?>> clauseContributor) {
		clauseContributor.accept( must() );
		return this;
	}

	default BooleanJunctionContext<N> mustNot(Consumer<QueryClauseContainerContext<?>> clauseContributor) {
		clauseContributor.accept( mustNot() );
		return this;
	}

	default BooleanJunctionContext<N> should(Consumer<QueryClauseContainerContext<?>> clauseContributor) {
		clauseContributor.accept( should() );
		return this;
	}

	default BooleanJunctionContext<N> filter(Consumer<QueryClauseContainerContext<?>> clauseContributor) {
		clauseContributor.accept( filter() );
		return this;
	}

}
