/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBase;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchTargetContext;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class LuceneIndexSearchTarget extends IndexSearchTargetBase {

	private final LuceneSearchTargetModel searchTargetModel;
	private final SearchTargetContext<?> searchTargetContext;

	LuceneIndexSearchTarget(SearchBackendContext searchBackendContext,
			Set<LuceneIndexModel> indexModels, Set<ReaderProvider> readerProviders) {
		this.searchTargetModel = new LuceneSearchTargetModel( indexModels, readerProviders );
		this.searchTargetContext = new LuceneSearchTargetContext( searchBackendContext, searchTargetModel );
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			SessionContext context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		return new SearchQueryResultDefinitionContextImpl<>( searchTargetContext, context,
				documentReferenceTransformer, objectLoader );
	}

	@Override
	protected SearchTargetContext<?> getSearchTargetContext() {
		return searchTargetContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( searchTargetModel.getIndexNames() )
				.append( "]")
				.toString();
	}
}
