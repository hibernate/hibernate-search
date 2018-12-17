/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetContextBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexManagerImpl implements IndexManagerImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchIndexManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final URLEncodedString elasticsearchIndexName;
	private final URLEncodedString typeName;
	private final ElasticsearchIndexModel model;

	private final ElasticsearchWorkOrchestrator workPlanOrchestrator;

	ElasticsearchIndexManagerImpl(IndexingBackendContext indexingBackendContext, SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName, URLEncodedString typeName,
			ElasticsearchIndexModel model) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.typeName = typeName;
		this.model = model;
		this.workPlanOrchestrator = indexingBackendContext.createWorkPlanOrchestrator();
	}

	@Override
	public void close() {
		// Index managers own the work plan context, but not the stream context (which is shared)
		workPlanOrchestrator.close();
	}

	public ElasticsearchIndexModel getModel() {
		return model;
	}

	@Override
	public IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(SessionContextImplementor sessionContext) {
		return indexingBackendContext.createWorkPlan( workPlanOrchestrator, elasticsearchIndexName, typeName, sessionContext );
	}

	@Override
	public IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(SessionContextImplementor sessionContext) {
		return indexingBackendContext.createDocumentWorkExecutor( workPlanOrchestrator, elasticsearchIndexName, typeName, sessionContext );
	}

	@Override
	public IndexWorkExecutor createWorkExecutor() {
		return indexingBackendContext.createWorkExecutor( elasticsearchIndexName );
	}

	@Override
	public IndexSearchTargetContextBuilder createSearchTargetContextBuilder(MappingContextImplementor mappingContext) {
		return new ElasticsearchIndexSearchTargetContextBuilder( searchBackendContext, mappingContext, this );
	}

	@Override
	public void addToSearchTarget(IndexSearchTargetContextBuilder searchTargetBuilder) {
		if ( !( searchTargetBuilder instanceof ElasticsearchIndexSearchTargetContextBuilder ) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherType(
					searchTargetBuilder, this, searchBackendContext.getEventContext()
			);
		}

		ElasticsearchIndexSearchTargetContextBuilder esSearchTargetBuilder = (ElasticsearchIndexSearchTargetContextBuilder) searchTargetBuilder;
		esSearchTargetBuilder.add( searchBackendContext, this );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( hibernateSearchIndexName )
				.append( "elasticsearchName=" ).append( elasticsearchIndexName.original )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchIndexManager.class ) ) {
			return (T) this;
		}
		throw log.indexManagerUnwrappingWithUnknownType(
				clazz, ElasticsearchIndexManager.class, getBackendAndIndexEventContext()
		);
	}

	private EventContext getBackendAndIndexEventContext() {
		return indexingBackendContext.getEventContext().append(
				EventContexts.fromIndexName( hibernateSearchIndexName )
		);
	}

}
