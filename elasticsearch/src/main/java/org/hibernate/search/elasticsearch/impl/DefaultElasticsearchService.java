/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategyFactory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchMissingValueStrategy;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.query.impl.ElasticsearchQueryFactory;
import org.hibernate.search.elasticsearch.schema.impl.DefaultElasticsearchSchemaCreator;
import org.hibernate.search.elasticsearch.schema.impl.DefaultElasticsearchSchemaDropper;
import org.hibernate.search.elasticsearch.schema.impl.DefaultElasticsearchSchemaMigrator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaCreator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaDropper;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaMigrator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.Closer;

/**
 * Provides access to the JEST client.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchService implements ElasticsearchService, Startable, Stoppable {

	private ElasticsearchClient client;

	private GsonProvider gsonProvider;

	private ElasticsearchWorkFactory workFactory;

	private ElasticsearchWorkProcessor workProcessor;

	private ElasticsearchSchemaCreator schemaCreator;

	private ElasticsearchSchemaDropper schemaDropper;

	private ElasticsearchSchemaMigrator schemaMigrator;

	private ElasticsearchSchemaValidator schemaValidator;

	private ElasticsearchSchemaTranslator schemaTranslator;

	private ElasticsearchAnalyzerStrategyFactory analyzerStrategyFactory;

	private MissingValueStrategy missingValueStrategy;

	private ElasticsearchQueryFactory queryFactory;

	private ElasticsearchQueryOptions queryOptions;

	@Override
	public void start(Properties unkmaskedProperties, BuildContext context) {
		Properties rootCfg = new MaskedProperty( unkmaskedProperties, "hibernate.search" );
		// Use root as a fallback to support query options in particular
		Properties properties = new MaskedProperty( rootCfg, "default", rootCfg );

		ServiceManager serviceManager = context.getServiceManager();

		this.queryOptions = createQueryOptions( properties );

		boolean logPrettyPrinting = ConfigurationParseHelper.getBooleanValue( properties,
				ElasticsearchEnvironment.LOG_JSON_PRETTY_PRINTING, ElasticsearchEnvironment.Defaults.LOG_JSON_PRETTY_PRINTING );

		ElasticsearchClientImplementor clientImplementor;
		try ( ServiceReference<ElasticsearchClientFactory> clientFactory =
				serviceManager.requestReference( ElasticsearchClientFactory.class ) ) {
			clientImplementor = clientFactory.get().create( properties );
		}

		try ( ServiceReference<ElasticsearchDialectFactory> dialectFactory =
				serviceManager.requestReference( ElasticsearchDialectFactory.class ) ) {
			ElasticsearchDialect dialect = dialectFactory.get().createDialect( clientImplementor, properties );
			this.gsonProvider = DefaultGsonProvider.create( dialect::createGsonBuilderBase, logPrettyPrinting );

			clientImplementor.init( gsonProvider );
			this.client = clientImplementor;

			this.workFactory = dialect.createWorkFactory( gsonProvider );

			this.workProcessor = new ElasticsearchWorkProcessor( context, client, gsonProvider, workFactory );

			ElasticsearchSchemaAccessor schemaAccessor = new ElasticsearchSchemaAccessor( workFactory, workProcessor );

			this.schemaValidator = dialect.createSchemaValidator( schemaAccessor );
			this.schemaTranslator = dialect.createSchemaTranslator();

			this.schemaCreator = new DefaultElasticsearchSchemaCreator( schemaAccessor );
			this.schemaDropper = new DefaultElasticsearchSchemaDropper( schemaAccessor );
			this.schemaMigrator = new DefaultElasticsearchSchemaMigrator( schemaAccessor, schemaValidator );

			this.analyzerStrategyFactory = dialect.createAnalyzerStrategyFactory( serviceManager );

			this.missingValueStrategy = new ElasticsearchMissingValueStrategy( schemaTranslator );

			this.queryFactory = dialect.createQueryFactory();
		}
	}

	@Override
	public void stop() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			if ( this.workProcessor != null ) {
				closer.push( this.workProcessor::close );
			}
			if ( this.client != null ) {
				closer.push( this.client::close );
			}
			this.client = null;
		}
		catch (IOException | RuntimeException e) {
			throw new SearchException( "Failed to shut down the Elasticsearch service", e );
		}
	}

	@Override
	public GsonProvider getGsonProvider() {
		return gsonProvider;
	}

	@Override
	public ElasticsearchWorkFactory getWorkFactory() {
		return workFactory;
	}

	@Override
	public ElasticsearchWorkProcessor getWorkProcessor() {
		return workProcessor;
	}

	@Override
	public ElasticsearchSchemaCreator getSchemaCreator() {
		return schemaCreator;
	}

	@Override
	public ElasticsearchSchemaDropper getSchemaDropper() {
		return schemaDropper;
	}

	@Override
	public ElasticsearchSchemaMigrator getSchemaMigrator() {
		return schemaMigrator;
	}

	@Override
	public ElasticsearchSchemaValidator getSchemaValidator() {
		return schemaValidator;
	}

	@Override
	public ElasticsearchSchemaTranslator getSchemaTranslator() {
		return schemaTranslator;
	}

	@Override
	public ElasticsearchAnalyzerStrategyFactory getAnalyzerStrategyFactory() {
		return analyzerStrategyFactory;
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return missingValueStrategy;
	}

	@Override
	public ElasticsearchQueryFactory getQueryFactory() {
		return queryFactory;
	}

	@Override
	public ElasticsearchQueryOptions getQueryOptions() {
		return queryOptions;
	}

	private ElasticsearchQueryOptions createQueryOptions(Properties properties) {
		String scrollTimeout = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.SCROLL_TIMEOUT,
				ElasticsearchEnvironment.Defaults.SCROLL_TIMEOUT
				) + "s";
		int scrollFetchSize = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.SCROLL_FETCH_SIZE,
				ElasticsearchEnvironment.Defaults.SCROLL_FETCH_SIZE
				);
		int scrollBacktrackingWindowSize = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.SCROLL_BACKTRACKING_WINDOW_SIZE,
				ElasticsearchEnvironment.Defaults.SCROLL_BACKTRACKING_WINDOW_SIZE
				);

		return new ElasticsearchQueryOptionsImpl( scrollTimeout, scrollFetchSize, scrollBacktrackingWindowSize );
	}

	private static class ElasticsearchQueryOptionsImpl implements ElasticsearchQueryOptions {
		private final String scrollTimeout;
		private final int scrollFetchSize;
		private final int scrollBacktrackingWindowSize;

		public ElasticsearchQueryOptionsImpl(String scrollTimeout, int scrollFetchSize, int scrollBacktrackingWindowSize) {
			super();
			this.scrollTimeout = scrollTimeout;
			this.scrollFetchSize = scrollFetchSize;
			this.scrollBacktrackingWindowSize = scrollBacktrackingWindowSize;
		}

		@Override
		public String getScrollTimeout() {
			return scrollTimeout;
		}

		@Override
		public int getScrollFetchSize() {
			return scrollFetchSize;
		}

		@Override
		public int getScrollBacktrackingWindowSize() {
			return scrollBacktrackingWindowSize;
		}

	}
}
