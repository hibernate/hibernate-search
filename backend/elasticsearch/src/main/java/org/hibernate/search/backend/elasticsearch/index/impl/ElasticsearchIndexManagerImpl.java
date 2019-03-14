/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.management.impl.ElasticsearchIndexLifecycleStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSharedWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchScopeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexManagerImpl implements IndexManagerImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchIndexManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> REFRESH_AFTER_WRITE =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.REFRESH_AFTER_WRITE )
					.asBoolean()
					.withDefault( ElasticsearchIndexSettings.Defaults.REFRESH_AFTER_WRITE )
					.build();

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final URLEncodedString elasticsearchIndexName;
	private final ElasticsearchIndexModel model;

	private final ElasticsearchIndexLifecycleStrategy managementStrategy;
	private final ElasticsearchIndexAdministrationClient administrationClient;

	private ElasticsearchSharedWorkOrchestrator serialOrchestrator;
	private ElasticsearchSharedWorkOrchestrator parallelOrchestrator;
	private boolean refreshAfterWrite;

	ElasticsearchIndexManagerImpl(IndexingBackendContext indexingBackendContext, SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			ElasticsearchIndexModel model,
			ElasticsearchIndexLifecycleStrategy managementStrategy) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.model = model;
		this.managementStrategy = managementStrategy;
		this.administrationClient = indexingBackendContext.createAdministrationClient(
				elasticsearchIndexName, model
		);
	}

	@Override
	public void start(IndexManagerStartContext context) {
		try {
			/*
			 * Create orchestrators late to allow the refresh_after_write setting to be changed
			 * after the first phase of bootstrap (useful for compile-time boot).
 			 */
			refreshAfterWrite = REFRESH_AFTER_WRITE.get( context.getConfigurationPropertySource() );
			parallelOrchestrator = indexingBackendContext.createParallelOrchestrator( elasticsearchIndexName.original );
			serialOrchestrator = indexingBackendContext.createSerialOrchestrator( elasticsearchIndexName.original, refreshAfterWrite );

			serialOrchestrator.start();
			parallelOrchestrator.start();
			managementStrategy.onStart( administrationClient, context );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( parallelOrchestrator )
					.push( serialOrchestrator );
			throw e;
		}
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchWorkOrchestrator::close, serialOrchestrator );
			serialOrchestrator = null;
			closer.push( ElasticsearchWorkOrchestrator::close, parallelOrchestrator );
			parallelOrchestrator = null;
			closer.push( strategy -> strategy.onStop( administrationClient ), managementStrategy );
		}
		catch (IOException e) {
			throw log.failedToShutdownIndexManager( hibernateSearchIndexName, e, indexingBackendContext.getEventContext() );
		}
	}

	public ElasticsearchIndexModel getModel() {
		return model;
	}

	@Override
	public IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(SessionContextImplementor sessionContext) {
		return indexingBackendContext.createWorkPlan(
				serialOrchestrator,
				elasticsearchIndexName,
				refreshAfterWrite,
				sessionContext
		);
	}

	@Override
	public IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(SessionContextImplementor sessionContext) {
		return indexingBackendContext.createDocumentWorkExecutor( parallelOrchestrator, elasticsearchIndexName, sessionContext );
	}

	@Override
	public IndexWorkExecutor createWorkExecutor() {
		return indexingBackendContext.createWorkExecutor( parallelOrchestrator, elasticsearchIndexName );
	}

	@Override
	public IndexSearchScopeBuilder createSearchScopeBuilder(MappingContextImplementor mappingContext) {
		return new ElasticsearchIndexSearchScopeBuilder( searchBackendContext, mappingContext, this );
	}

	@Override
	public void addTo(IndexSearchScopeBuilder builder) {
		if ( !( builder instanceof ElasticsearchIndexSearchScopeBuilder ) ) {
			throw log.cannotMixElasticsearchSearchScopeWithOtherType(
					builder, this, searchBackendContext.getEventContext()
			);
		}

		ElasticsearchIndexSearchScopeBuilder esBuilder = (ElasticsearchIndexSearchScopeBuilder) builder;
		esBuilder.add( searchBackendContext, this );
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
	@SuppressWarnings("unchecked") // Checked using reflection
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
