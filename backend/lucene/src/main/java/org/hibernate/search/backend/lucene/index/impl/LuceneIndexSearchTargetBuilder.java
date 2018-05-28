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

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class LuceneIndexSearchTargetBuilder implements IndexSearchTargetBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<LuceneIndexManager> indexManagers = new LinkedHashSet<>();

	LuceneIndexSearchTargetBuilder(SearchBackendContext searchBackendContext, LuceneIndexManager indexManager) {
		this.searchBackendContext = searchBackendContext;
		this.indexManagers.add( indexManager );
	}

	void add(SearchBackendContext searchBackendContext, LuceneIndexManager indexManager) {
		if ( ! this.searchBackendContext.equals( searchBackendContext ) ) {
			throw log.cannotMixLuceneSearchTargetWithOtherBackend( this, indexManager );
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexSearchTarget build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<LuceneIndexModel> indexModels = indexManagers.stream().map( LuceneIndexManager::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );

		// TODO obviously, this will have to be changed once we have the full storage complexity from Search 5
		Set<ReaderProvider> readerProviders = indexManagers.stream().map( LuceneIndexManager::getReaderProvider )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );

		return new LuceneIndexSearchTarget( searchBackendContext, indexModels, readerProviders );
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
