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
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class ElasticsearchIndexScopeBuilder implements IndexScopeBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;
	private final MappingContextImplementor mappingContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	ElasticsearchIndexScopeBuilder(SearchBackendContext searchBackendContext,
			MappingContextImplementor mappingContext, ElasticsearchIndexManagerImpl indexManager) {
		this.searchBackendContext = searchBackendContext;
		this.mappingContext = mappingContext;
		this.indexManagers.add( indexManager );
	}

	void add(SearchBackendContext searchBackendContext, ElasticsearchIndexManagerImpl indexManager) {
		if ( ! this.searchBackendContext.equals( searchBackendContext ) ) {
			throw log.cannotMixElasticsearchScopeWithOtherBackend(
					this, indexManager, searchBackendContext.getEventContext()
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
		return new ElasticsearchIndexScope( mappingContext, searchBackendContext, model );
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
