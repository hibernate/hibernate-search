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
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchDialectName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.MultiTenancyStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
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
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryContextProvider;
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
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<ElasticsearchDialectName> DIALECT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.DIALECT )
					.as( ElasticsearchDialectName.class, ElasticsearchDialectName::of )
					.withDefault( ElasticsearchBackendSettings.Defaults.DIALECT )
					.build();

	private static final ConfigurationProperty<MultiTenancyStrategyName> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyName.class, MultiTenancyStrategyName::of )
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
		/*
		 * The Elasticsearch client only converts JsonObjects to String and
		 * vice-versa, it doesn't need a Gson instance that was specially
		 * configured for a particular Elasticsearch version.
		 */
		GsonProvider defaultGsonProvider = DefaultGsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		ElasticsearchDialectName dialectName = DIALECT.get( propertySource );

		BeanProvider beanProvider = buildContext.getBeanProvider();
		BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder = null;
		ElasticsearchClientImplementor client = null;
		try {
			clientFactoryHolder = CLIENT_FACTORY.getAndTransform( propertySource, beanProvider::getBean );

			ElasticsearchClientProvider clientProvider;
			ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();
			ElasticsearchDialect dialect;
			if ( ElasticsearchDialectName.AUTO.equals( dialectName ) ) {
				// We must determine the appropriate dialect, and thus instantiate the client, right now.
				client = clientFactoryHolder.get().create( propertySource, defaultGsonProvider );
				clientFactoryHolder.close(); // We won't need this anymore
				clientProvider = new ElasticsearchClientProvider( client );

				ElasticsearchVersion version = ElasticsearchClientUtils.getElasticsearchVersion( client );
				dialectName = dialectFactory.getAppropriateDialectName( version );
				dialect = dialectFactory.create( dialectName );
			}
			else {
				// We can delay the client instantiation to when the backend starts; we'll check that the dialect is appropriate then.
				clientProvider = new ElasticsearchClientProvider(
						clientFactoryHolder, defaultGsonProvider, dialectFactory, dialectName
				);

				dialect = dialectFactory.create( dialectName );
			}

			GsonProvider dialectSpecificGsonProvider =
					DefaultGsonProvider.create( dialect::createGsonBuilderBase, logPrettyPrinting );

			Gson userFacingGson = new GsonBuilder().setPrettyPrinting().create();

			ElasticsearchWorkBuilderFactory workFactory = dialect.createWorkBuilderFactory( dialectSpecificGsonProvider );

			ElasticsearchIndexFieldTypeFactoryContextProvider typeFactoryContextProvider =
					dialect.createIndexTypeFieldFactoryContextProvider( userFacingGson );

			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory =
					dialect.createSearchResultExtractorFactory();

			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry =
					getAnalysisDefinitionRegistry( backendContext, buildContext, propertySource );

			return new ElasticsearchBackendImpl(
					clientProvider,
					dialectSpecificGsonProvider,
					name,
					workFactory,
					typeFactoryContextProvider,
					searchResultExtractorFactory,
					userFacingGson,
					analysisDefinitionRegistry,
					getMultiTenancyStrategy( name, propertySource )
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( BeanHolder::close, clientFactoryHolder )
					.push( ElasticsearchClientImplementor::close, client );
			throw e;
		}
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
			final BeanProvider beanProvider = buildContext.getBeanProvider();
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
