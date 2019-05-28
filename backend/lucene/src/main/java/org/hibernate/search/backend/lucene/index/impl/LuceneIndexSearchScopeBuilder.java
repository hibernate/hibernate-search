/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchScopeModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneIndexSearchScope;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class LuceneIndexSearchScopeBuilder implements IndexSearchScopeBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;
	private final MappingContextImplementor mappingContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<LuceneIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	LuceneIndexSearchScopeBuilder(SearchBackendContext searchBackendContext, MappingContextImplementor mappingContext,
			LuceneIndexManagerImpl indexManager) {
		this.searchBackendContext = searchBackendContext;
		this.mappingContext = mappingContext;
		this.indexManagers.add( indexManager );
	}

	void add(SearchBackendContext searchBackendContext, LuceneIndexManagerImpl indexManager) {
		if ( ! this.searchBackendContext.equals( searchBackendContext ) ) {
			throw log.cannotMixLuceneSearchScopeWithOtherBackend(
					this, indexManager, searchBackendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexSearchScope<?> build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<LuceneIndexModel> indexModels = indexManagers.stream().map( LuceneIndexManagerImpl::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );

		// TODO HSEARCH-3117 obviously, this will have to be changed once we have the full storage complexity from Search 5
		Set<ReaderProvider> readerProviders = indexManagers.stream().map( LuceneIndexManagerImpl::getReaderProvider )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );

		LuceneSearchScopeModel model = new LuceneSearchScopeModel( indexModels, readerProviders );

		return new LuceneIndexSearchScope( searchBackendContext, mappingContext, model );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "searchBackendContext=" ).append( searchBackendContext )
				.append( ", indexManagers=" ).append( indexManagers )
				.append( "]" )
				.toString();
	}

}
