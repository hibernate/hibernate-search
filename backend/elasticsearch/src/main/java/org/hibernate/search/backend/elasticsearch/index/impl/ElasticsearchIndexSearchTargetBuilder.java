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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSearchTargetBuilder implements IndexSearchTargetBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	ElasticsearchIndexSearchTargetBuilder(SearchBackendContext searchBackendContext, ElasticsearchIndexManagerImpl indexManager) {
		this.searchBackendContext = searchBackendContext;
		this.indexManagers.add( indexManager );
	}

	void add(SearchBackendContext searchBackendContext, ElasticsearchIndexManagerImpl indexManager) {
		if ( ! this.searchBackendContext.equals( searchBackendContext ) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherBackend(
					this, indexManager, searchBackendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexSearchTarget build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<ElasticsearchIndexModel> indexModels = indexManagers.stream().map( ElasticsearchIndexManagerImpl::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		ElasticsearchSearchTargetModel searchTargetModel = new ElasticsearchSearchTargetModel( indexModels );
		return new ElasticsearchIndexSearchTarget( searchBackendContext, searchTargetModel );
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
