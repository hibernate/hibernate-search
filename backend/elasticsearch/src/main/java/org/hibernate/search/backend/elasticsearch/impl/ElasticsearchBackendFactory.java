/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl.ElasticsearchAnalysisConfigurationContextImpl;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.DefaultIndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.mapping.TypeNameMappingStrategyName;
import org.hibernate.search.backend.elasticsearch.mapping.impl.DiscriminatorTypeNameMapping;
import org.hibernate.search.backend.elasticsearch.mapping.impl.IndexNameTypeNameMapping;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class ElasticsearchBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<ElasticsearchVersion> VERSION =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.VERSION )
					.as( ElasticsearchVersion.class, ElasticsearchVersion::of )
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

	private static final ConfigurationProperty<TypeNameMappingStrategyName> MAPPING_TYPE_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY )
					.as( TypeNameMappingStrategyName.class, TypeNameMappingStrategyName::of )
					.withDefault( ElasticsearchBackendSettings.Defaults.MAPPING_TYPE_NAME_STRATEGY )
					.build();

	private static final ConfigurationProperty<BeanReference<? extends IndexLayoutStrategy>> LAYOUT_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.LAYOUT_STRATEGY )
					.asBeanReference( IndexLayoutStrategy.class )
					.withDefault( BeanReference.of( DefaultIndexLayoutStrategy.class ) )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		/*
		 * The Elasticsearch client only converts JsonObjects to String and
		 * vice-versa, it doesn't need a Gson instance that was specially
		 * configured for a particular Elasticsearch version.
		 */
		GsonProvider defaultGsonProvider = GsonProvider.create( GsonBuilder::new, logPrettyPrinting );

		Optional<ElasticsearchVersion> configuredVersion = VERSION.get( propertySource );

		BeanResolver beanResolver = buildContext.getBeanResolver();
		BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder = null;
		BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder = null;
		ElasticsearchLinkImpl link = null;
		try {
			clientFactoryHolder = CLIENT_FACTORY.getAndTransform( propertySource, beanResolver::resolve );

			ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();
			link = new ElasticsearchLinkImpl(
					clientFactoryHolder, buildContext.getThreadPoolProvider(), defaultGsonProvider, logPrettyPrinting,
					dialectFactory, configuredVersion
			);

			ElasticsearchModelDialect dialect;
			ElasticsearchVersion version;
			if ( configuredVersion.isPresent() ) {
				version = configuredVersion.get();
			}
			else {
				// We must determine the Elasticsearch version, and thus instantiate the client, right now.
				link.onStart( propertySource );

				version = link.getElasticsearchVersion();
			}

			dialect = dialectFactory.createModelDialect( version );

			Gson userFacingGson = new GsonBuilder().setPrettyPrinting().create();

			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider =
					dialect.createIndexTypeFieldFactoryProvider( userFacingGson );

			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry =
					getAnalysisDefinitionRegistry( buildContext, propertySource );

			indexLayoutStrategyHolder = createIndexLayoutStrategy( buildContext, propertySource );

			return new ElasticsearchBackendImpl(
					name,
					link,
					buildContext.getThreadPoolProvider(),
					typeFactoryProvider,
					userFacingGson,
					analysisDefinitionRegistry,
					getMultiTenancyStrategy( name, propertySource ),
					indexLayoutStrategyHolder,
					createTypeNameMapping( name, propertySource, indexLayoutStrategyHolder.get() ),
					buildContext.getFailureHandler()
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( BeanHolder::close, clientFactoryHolder )
					.push( BeanHolder::close, indexLayoutStrategyHolder )
					.push( ElasticsearchLinkImpl::onStop, link );
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

	private BeanHolder<? extends IndexLayoutStrategy> createIndexLayoutStrategy(BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		final BeanResolver beanResolver = buildContext.getBeanResolver();
		return LAYOUT_STRATEGY.get( propertySource ).resolve( beanResolver );
	}

	private TypeNameMapping createTypeNameMapping(String backendName, ConfigurationPropertySource propertySource,
			IndexLayoutStrategy indexLayoutStrategy) {
		TypeNameMappingStrategyName strategyName = MAPPING_TYPE_STRATEGY.get( propertySource );

		switch ( strategyName ) {
			case INDEX_NAME:
				return new IndexNameTypeNameMapping( indexLayoutStrategy );
			case DISCRIMINATOR:
				return new DiscriminatorTypeNameMapping();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported type mapping strategy '%2$s' for backend '%1$s'",
						backendName, strategyName
				) );
		}
	}

	private ElasticsearchAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		try {
			// Apply the user-provided analysis configurer if necessary
			final BeanResolver beanResolver = buildContext.getBeanResolver();
			return ANALYSIS_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
					.map( holder -> {
						try ( BeanHolder<? extends ElasticsearchAnalysisConfigurer> configurerHolder = holder ) {
							ElasticsearchAnalysisConfigurationContextImpl collector =
									new ElasticsearchAnalysisConfigurationContextImpl();
							configurerHolder.get().configure( collector );
							return new ElasticsearchAnalysisDefinitionRegistry( collector );
						}
					} )
					// Otherwise just use an empty registry
					.orElseGet( ElasticsearchAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), e );
		}
	}
}
