/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchRootIndexSchemaCollectorImpl;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexingBackendContext;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.StubElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchBackendImpl implements BackendImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchClient client;

	private final String name;

	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator streamOrchestrator;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;

	private final IndexingBackendContext indexingContext;
	private final SearchBackendContext searchContext;

	ElasticsearchBackendImpl(ElasticsearchClient client, String name, ElasticsearchWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy) {
		this.client = client;
		this.name = name;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.streamOrchestrator = new StubElasticsearchWorkOrchestrator( client );
		this.queryOrchestrator = new StubElasticsearchWorkOrchestrator( client );
		this.indexingContext = new IndexingBackendContext(
				this, client, workFactory, multiTenancyStrategy, streamOrchestrator
		);
		this.searchContext = new SearchBackendContext(
				this, workFactory, multiTenancyStrategy, queryOrchestrator
		);
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		if ( ElasticsearchBackend.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		throw log.backendUnwrappingWithUnknownType( clazz, ElasticsearchBackend.class );
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	@Override
	public <T> T getClient(Class<T> clientClass) {
		return client.unwrap( clientClass );
	}

	@Override
	public String normalizeIndexName(String rawIndexName) {
		return ElasticsearchIndexNameNormalizer.normalize( rawIndexName );
	}

	@Override
	public IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> createIndexManagerBuilder(
			String normalizedIndexName, boolean multiTenancyEnabled, BuildContext buildContext, ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( name, normalizedIndexName );
		}

		ElasticsearchRootIndexSchemaCollectorImpl schemaCollector =
				new ElasticsearchRootIndexSchemaCollectorImpl( multiTenancyStrategy );

		return new ElasticsearchIndexManagerBuilder(
				indexingContext, searchContext, normalizedIndexName, schemaCollector, buildContext, propertySource
		);
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( streamOrchestrator::close );
			closer.push( queryOrchestrator::close );
			// Close the index writer after the orchestrators, when we're sure all works have been performed
			closer.push( client::close );
		}
		catch (IOException | RuntimeException e) {
			throw new SearchException( "Failed to shut down the Elasticsearch backend", e );
		}
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name )
				.append( "]")
				.toString();
	}
}
