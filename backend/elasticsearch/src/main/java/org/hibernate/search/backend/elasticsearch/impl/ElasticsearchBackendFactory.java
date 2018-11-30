/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl.ElasticsearchAnalysisDefinitionContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.cfg.MultiTenancyStrategyConfiguration;
import org.hibernate.search.backend.elasticsearch.cfg.SearchBackendElasticsearchSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
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
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchStubWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;

import com.google.gson.GsonBuilder;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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

	private static final OptionalConfigurationProperty<BeanReference<? extends ElasticsearchAnalysisConfigurer>> ANALYSIS_CONFIGURER =
			ConfigurationProperty.forKey( SearchBackendElasticsearchSettings.ANALYSIS_CONFIGURER )
					.asBeanReference( ElasticsearchAnalysisConfigurer.class )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		EventContext backendContext = EventContexts.fromBackendName( name );

		ElasticsearchClientFactory clientFactory = new ElasticsearchClientFactoryImpl();

		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		GsonProvider initialGsonProvider = DefaultGsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		ElasticsearchClientImplementor client = clientFactory.create( propertySource, initialGsonProvider );
		try {
			// TODO implement and detect dialects
			// Assume ES5 for now
			GsonProvider dialectSpecificGsonProvider =
					DefaultGsonProvider.create( this::createES5GsonBuilderBase, logPrettyPrinting );
			client.init( dialectSpecificGsonProvider );

			MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( name, propertySource );
			ElasticsearchWorkFactory workFactory = new ElasticsearchStubWorkFactory( dialectSpecificGsonProvider, multiTenancyStrategy );

			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry =
					getAnalysisDefinitionRegistry( backendContext, buildContext, propertySource );

			return new ElasticsearchBackendImpl(
					client, name, workFactory,
					analysisDefinitionRegistry,
					multiTenancyStrategy
			);
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
				return new NoMultiTenancyStrategy();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategy();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported multi-tenancy strategy '%2$s' for backend '%1$s'",
						backendName, multiTenancyStrategyConfiguration
				) );
		}
	}

	private ElasticsearchAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(EventContext backendContext,
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		try {
			// Apply the user-provided analysis configurer if necessary
			final BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
			return ANALYSIS_CONFIGURER.getAndMap( propertySource, beanProvider::getBean )
					.map( configurer -> {
						ElasticsearchAnalysisDefinitionContainerContextImpl collector
								= new ElasticsearchAnalysisDefinitionContainerContextImpl();
						configurer.configure( collector );
						return new ElasticsearchAnalysisDefinitionRegistry( collector );
					} )
					// Otherwise just use an empty registry
					.orElseGet( ElasticsearchAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), backendContext, e );
		}
	}
}
