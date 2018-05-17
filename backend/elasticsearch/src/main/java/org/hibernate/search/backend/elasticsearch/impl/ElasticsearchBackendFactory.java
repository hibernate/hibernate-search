/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import org.hibernate.search.backend.elasticsearch.cfg.MultiTenancyStrategyConfiguration;
import org.hibernate.search.backend.elasticsearch.cfg.SearchBackendElasticsearchSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.DefaultElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.FieldDataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.IndexType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.NormsType;
import org.hibernate.search.backend.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.backend.elasticsearch.gson.impl.ES5FieldDataTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.gson.impl.ES5IndexTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.gson.impl.ES5NormsTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.DiscriminatorMultiTenancyStrategyImpl;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.NoMultiTenancyStrategyImpl;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.StubElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.SuppressingCloser;

import com.google.gson.GsonBuilder;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendFactory implements BackendFactory {

	private static final ConfigurationProperty<MultiTenancyStrategyConfiguration> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyConfiguration.class, MultiTenancyStrategyConfiguration::fromExternalRepresentation )
					.withDefault( SearchBackendElasticsearchSettings.Defaults.MULTI_TENANCY_STRATEGY )
					.build();

	private static final ConfigurationProperty<Boolean> LOG_JSON_PRETTY_PRINTING =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.LOG_JSON_PRETTY_PRINTING )
					.asBoolean()
					.withDefault( SearchBackendElasticsearchSettings.Defaults.LOG_JSON_PRETTY_PRINTING )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BuildContext context, ConfigurationPropertySource propertySource) {
		ElasticsearchClientFactory clientFactory = new DefaultElasticsearchClientFactory();

		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		GsonProvider initialGsonProvider = DefaultGsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		ElasticsearchClientImplementor client = clientFactory.create( propertySource, initialGsonProvider );
		try {
			// TODO implement and detect dialects
			// Assume ES5 for now
			GsonProvider dialectSpecificGsonProvider =
					DefaultGsonProvider.create( this::createES5GsonBuilderBase, logPrettyPrinting );
			client.init( dialectSpecificGsonProvider );

			ElasticsearchWorkFactory workFactory = new StubElasticsearchWorkFactory( dialectSpecificGsonProvider );

			return new ElasticsearchBackendImpl(
					client, name, workFactory, getMultiTenancyStrategy( name, propertySource ) );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( ElasticsearchClientImplementor::close, client );
			throw e;
		}
	}

	private GsonBuilder createES5GsonBuilderBase() {
		return new GsonBuilder()
				.registerTypeAdapter( IndexType.class, new ES5IndexTypeJsonAdapter().nullSafe() )
				.registerTypeAdapter( FieldDataType.class, new ES5FieldDataTypeJsonAdapter().nullSafe() )
				.registerTypeAdapter( NormsType.class, new ES5NormsTypeJsonAdapter().nullSafe() );
	}

	private MultiTenancyStrategy getMultiTenancyStrategy(String backendName, ConfigurationPropertySource propertySource) {
		MultiTenancyStrategyConfiguration multiTenancyStrategyConfiguration = MULTI_TENANCY_STRATEGY.get( propertySource );

		switch ( multiTenancyStrategyConfiguration ) {
			case NONE:
				return new NoMultiTenancyStrategyImpl();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategyImpl();
			default:
				throw new AssertionFailure( String.format( "Unsupported multi-tenancy strategy '%2$s' for backend '%1$s'", backendName,
						multiTenancyStrategyConfiguration ) );
		}
	}
}
