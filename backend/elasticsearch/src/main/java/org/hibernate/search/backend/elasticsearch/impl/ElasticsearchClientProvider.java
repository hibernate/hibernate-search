/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchDialectName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

class ElasticsearchClientProvider implements Supplier<ElasticsearchClient> {

	private final BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder;
	private final GsonProvider defaultGsonProvider;
	private final ElasticsearchDialectFactory dialectFactory;
	private final ElasticsearchDialectName configuredDialectName;

	private ElasticsearchClientImplementor clientImplementor;
	private ElasticsearchVersion elasticsearchVersion;

	ElasticsearchClientProvider(BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder,
			GsonProvider defaultGsonProvider,
			ElasticsearchDialectFactory dialectFactory,
			ElasticsearchDialectName configuredDialectName) {
		this.clientFactoryHolder = clientFactoryHolder;
		this.defaultGsonProvider = defaultGsonProvider;
		this.dialectFactory = dialectFactory;
		this.configuredDialectName = configuredDialectName;
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

			if ( !ElasticsearchDialectName.AUTO.equals( configuredDialectName ) ) {
				dialectFactory.checkAppropriate( configuredDialectName, elasticsearchVersion );
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
