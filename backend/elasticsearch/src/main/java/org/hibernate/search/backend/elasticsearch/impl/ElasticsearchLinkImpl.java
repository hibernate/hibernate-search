/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.backend.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchJsonSyntaxHelper;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchLinkImpl implements ElasticsearchLink {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder;
	private final GsonProvider defaultGsonProvider;
	private final boolean logPrettyPrinting;
	private final ElasticsearchDialectFactory dialectFactory;
	private final Optional<ElasticsearchVersion> configuredVersionOptional;

	private ElasticsearchClientImplementor clientImplementor;
	private ElasticsearchVersion elasticsearchVersion;
	private GsonProvider gsonProvider;
	private ElasticsearchJsonSyntaxHelper jsonSyntaxHelper;
	private ElasticsearchWorkBuilderFactory workBuilderFactory;
	private ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;

	ElasticsearchLinkImpl(BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder,
			GsonProvider defaultGsonProvider, boolean logPrettyPrinting,
			ElasticsearchDialectFactory dialectFactory,
			Optional<ElasticsearchVersion> configuredVersionOptional) {
		this.clientFactoryHolder = clientFactoryHolder;
		this.defaultGsonProvider = defaultGsonProvider;
		this.logPrettyPrinting = logPrettyPrinting;
		this.dialectFactory = dialectFactory;
		this.configuredVersionOptional = configuredVersionOptional;
	}

	@Override
	public ElasticsearchClient getClient() {
		checkStarted();
		return clientImplementor;
	}

	@Override
	public GsonProvider getGsonProvider() {
		checkStarted();
		return gsonProvider;
	}

	@Override
	public ElasticsearchJsonSyntaxHelper getJsonSyntaxHelper() {
		return jsonSyntaxHelper;
	}

	@Override
	public ElasticsearchWorkBuilderFactory getWorkBuilderFactory() {
		checkStarted();
		return workBuilderFactory;
	}

	@Override
	public ElasticsearchSearchResultExtractorFactory getSearchResultExtractorFactory() {
		checkStarted();
		return searchResultExtractorFactory;
	}

	ElasticsearchVersion getElasticsearchVersion() {
		checkStarted();
		return elasticsearchVersion;
	}

	void onStart(ConfigurationPropertySource propertySource) {
		if ( clientImplementor == null ) {
			clientImplementor = clientFactoryHolder.get().create( propertySource, defaultGsonProvider );
			clientFactoryHolder.close(); // We won't need it anymore

			elasticsearchVersion = ElasticsearchClientUtils.getElasticsearchVersion( clientImplementor );
			if ( configuredVersionOptional.isPresent() ) {
				ElasticsearchVersion configuredVersion = configuredVersionOptional.get();
				if ( !configuredVersion.matches( elasticsearchVersion ) ) {
					throw log.unexpectedElasticsearchVersion( configuredVersion, elasticsearchVersion );
				}
			}

			ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( elasticsearchVersion );
			gsonProvider = DefaultGsonProvider.create( protocolDialect::createGsonBuilderBase, logPrettyPrinting );
			jsonSyntaxHelper = protocolDialect.createJsonSyntaxHelper();
			workBuilderFactory = protocolDialect.createWorkBuilderFactory( gsonProvider );
			searchResultExtractorFactory = protocolDialect.createSearchResultExtractorFactory();
		}
	}

	void onStop() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, clientFactoryHolder ); // Just in case start() was not called
			closer.push( ElasticsearchClientImplementor::close, clientImplementor );
		}
	}

	private void checkStarted() {
		if ( clientImplementor == null ) {
			throw new AssertionFailure(
					"Attempt to retrieve Elasticsearch client or related information before the backend was started."
							+ "There is probably a bug in Hibernate Search, please report it."
			);
		}
	}
}
