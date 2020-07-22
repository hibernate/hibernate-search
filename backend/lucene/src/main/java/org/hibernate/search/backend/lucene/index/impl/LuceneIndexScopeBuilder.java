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

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeSearchIndexesContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class LuceneIndexScopeBuilder implements IndexScopeBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexManagerBackendContext backendContext;
	private final BackendMappingContext mappingContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<LuceneIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	LuceneIndexScopeBuilder(IndexManagerBackendContext backendContext, BackendMappingContext mappingContext,
			LuceneIndexManagerImpl indexManager) {
		this.backendContext = backendContext;
		this.mappingContext = mappingContext;
		this.indexManagers.add( indexManager );
	}

	void add(IndexManagerBackendContext backendContext, LuceneIndexManagerImpl indexManager) {
		if ( ! this.backendContext.equals( backendContext ) ) {
			throw log.cannotMixLuceneScopeWithOtherBackend(
					this, indexManager, backendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexScope<?> build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<LuceneScopeIndexManagerContext> indexManagerContexts = new LinkedHashSet<>( indexManagers );

		LuceneScopeSearchIndexesContext model = new LuceneScopeSearchIndexesContext( indexManagerContexts );

		return new LuceneIndexScope( backendContext, mappingContext, model );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "backendContext=" ).append( backendContext )
				.append( ", indexManagers=" ).append( indexManagers )
				.append( "]" )
				.toString();
	}

}
