/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import java.util.function.Function;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionContainerContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public interface IndexSearchTarget {

	default SearchQueryResultDefinitionContext<DocumentReference, DocumentReference> query(SessionContext context) {
		return query( context, Function.identity(), ObjectLoader.identity() );
	}

	<R, O> SearchQueryResultDefinitionContext<R, O> query(SessionContext context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader);

	SearchPredicateContainerContext<SearchPredicate> predicate();

	SearchSortContainerContext<SearchSort> sort();

	SearchProjectionContainerContext projection();

}
