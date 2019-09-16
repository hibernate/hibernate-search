/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class ElasticsearchIndexScopeBuilder implements IndexScopeBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexManagerBackendContext backendContext;
	private final BackendMappingContext mappingContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	ElasticsearchIndexScopeBuilder(IndexManagerBackendContext backendContext,
			BackendMappingContext mappingContext, ElasticsearchIndexManagerImpl indexManager) {
		this.backendContext = backendContext;
		this.mappingContext = mappingContext;
		this.indexManagers.add( indexManager );
	}

	void add(IndexManagerBackendContext backendContext, ElasticsearchIndexManagerImpl indexManager) {
		if ( ! this.backendContext.equals( backendContext ) ) {
			throw log.cannotMixElasticsearchScopeWithOtherBackend(
					this, indexManager, backendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexScope<?> build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<ElasticsearchIndexModel> indexModels = indexManagers.stream().map( ElasticsearchIndexManagerImpl::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		ElasticsearchScopeModel model = new ElasticsearchScopeModel( indexModels );
		return new ElasticsearchIndexScope( mappingContext, backendContext, model );
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
