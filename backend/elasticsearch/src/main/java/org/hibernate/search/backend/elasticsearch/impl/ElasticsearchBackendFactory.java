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
import org.hibernate.search.backend.elasticsearch.cfg.MultiTenancyStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<MultiTenancyStrategyName> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyName.class, MultiTenancyStrategyName::fromExternalRepresentation )
					.withDefault( ElasticsearchBackendSettings.Defaults.MULTI_TENANCY_STRATEGY )
					.build();

	private static final ConfigurationProperty<Boolean> LOG_JSON_PRETTY_PRINTING =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.LOG_JSON_PRETTY_PRINTING )
					.asBoolean()
					.withDefault( ElasticsearchBackendSettings.Defaults.LOG_JSON_PRETTY_PRINTING )
					.build();

	private static final ConfigurationProperty<BeanReference<? extends ElasticsearchClientFactory>> CLIENT_FACTORY =
			ConfigurationProperty.forKey( ElasticsearchBackendSpiSettings.CLIENT_FACTORY )
					.asBeanReference( ElasticsearchClientFactory.class )
					.withDefault( ElasticsearchBackendSpiSettings.Defaults.CLIENT_FACTORY )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends ElasticsearchAnalysisConfigurer>> ANALYSIS_CONFIGURER =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.ANALYSIS_CONFIGURER )
					.asBeanReference( ElasticsearchAnalysisConfigurer.class )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		EventContext backendContext = EventContexts.fromBackendName( name );

		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		GsonProvider initialGsonProvider = DefaultGsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		ElasticsearchClientImplementor client = null;
		try {
			BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
			try ( BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder =
					CLIENT_FACTORY.getAndTransform( propertySource, beanProvider::getBean ) ) {
				client = clientFactoryHolder.get().create( propertySource, initialGsonProvider );
			}

			ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();
			ElasticsearchDialect dialect = dialectFactory.createFromClusterVersion( client );

			GsonProvider dialectSpecificGsonProvider =
					DefaultGsonProvider.create( dialect::createGsonBuilderBase, logPrettyPrinting );
			client.init( dialectSpecificGsonProvider );

			Gson userFacingGson = new GsonBuilder().setPrettyPrinting().create();

			ElasticsearchWorkBuilderFactory workFactory = dialect.createWorkBuilderFactory( dialectSpecificGsonProvider );

			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry =
					getAnalysisDefinitionRegistry( backendContext, buildContext, propertySource );

			return new ElasticsearchBackendImpl(
					client, dialectSpecificGsonProvider, name, workFactory, userFacingGson,
					analysisDefinitionRegistry,
					getMultiTenancyStrategy( name, propertySource )
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( ElasticsearchClientImplementor::close, client );
			throw e;
		}
	}

	private GsonBuilder createES5GsonBuilderBase() {
		return new GsonBuilder();
	}

	private MultiTenancyStrategy getMultiTenancyStrategy(String backendName, ConfigurationPropertySource propertySource) {
		MultiTenancyStrategyName multiTenancyStrategyName = MULTI_TENANCY_STRATEGY.get( propertySource );

		switch ( multiTenancyStrategyName ) {
			case NONE:
				return new NoMultiTenancyStrategy();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategy();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported multi-tenancy strategy '%2$s' for backend '%1$s'",
						backendName, multiTenancyStrategyName
				) );
		}
	}

	private ElasticsearchAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(EventContext backendContext,
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		try {
			// Apply the user-provided analysis configurer if necessary
			final BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
			return ANALYSIS_CONFIGURER.getAndMap( propertySource, beanProvider::getBean )
					.map( holder -> {
						try ( BeanHolder<? extends ElasticsearchAnalysisConfigurer> configurerHolder = holder ) {
							ElasticsearchAnalysisDefinitionContainerContextImpl collector =
									new ElasticsearchAnalysisDefinitionContainerContextImpl();
							configurerHolder.get().configure( collector );
							return new ElasticsearchAnalysisDefinitionRegistry( collector );
						}
					} )
					// Otherwise just use an empty registry
					.orElseGet( ElasticsearchAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), backendContext, e );
		}
	}
}
