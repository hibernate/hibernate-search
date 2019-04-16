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
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ElasticsearchClientProvider implements Supplier<ElasticsearchClient> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder;
	private final GsonProvider defaultGsonProvider;
	private final Optional<ElasticsearchVersion> configuredVersionOptional;

	private ElasticsearchClientImplementor clientImplementor;
	private ElasticsearchVersion elasticsearchVersion;

	ElasticsearchClientProvider(BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder,
			GsonProvider defaultGsonProvider,
			Optional<ElasticsearchVersion> configuredVersionOptional) {
		this.clientFactoryHolder = clientFactoryHolder;
		this.defaultGsonProvider = defaultGsonProvider;
		this.configuredVersionOptional = configuredVersionOptional;
	}

	@Override
	public ElasticsearchClient get() {
		checkStarted();
		return clientImplementor;
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
					"Attempt to retrieve Elasticsearch client or version before the backend was started."
							+ "There is probably a bug in Hibernate Search, please report it."
			);
		}
	}
}
