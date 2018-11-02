/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public interface IndexSearchTarget {

	default SearchQueryResultDefinitionContext<DocumentReference, DocumentReference> query(
			SessionContextImplementor context) {
		return query( context, Function.identity(), ObjectLoader.identity() );
	}

	<R, O> SearchQueryResultDefinitionContext<R, O> query(SessionContextImplementor context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext projection();

}
