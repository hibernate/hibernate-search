package org.hibernate.search.backend.elasticsearch.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ClientJdkElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.logging.spi.ConfigurationLog;
import org.hibernate.search.backend.elasticsearch.mapping.TypeNameMappingStrategyName;
import org.hibernate.search.backend.elasticsearch.mapping.impl.DiscriminatorTypeNameMapping;
import org.hibernate.search.backend.elasticsearch.mapping.impl.IndexNameTypeNameMapping;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

public class ElasticsearchBackendFactory implements BackendFactory {

	private static final OptionalConfigurationProperty<MultiTenancyStrategyName> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyName.class, MultiTenancyStrategyName::of )
					.build();

	private static final ConfigurationProperty<Boolean> LOG_JSON_PRETTY_PRINTING =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.LOG_JSON_PRETTY_PRINTING )
					.asBoolean()
					.withDefault( ElasticsearchBackendSettings.Defaults.LOG_JSON_PRETTY_PRINTING )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends ElasticsearchClientFactory>> CLIENT_FACTORY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.CLIENT_FACTORY )
					.asBeanReference( ElasticsearchClientFactory.class )
					.build();

	private static final ConfigurationProperty<TypeNameMappingStrategyName> MAPPING_TYPE_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY )
					.as( TypeNameMappingStrategyName.class, TypeNameMappingStrategyName::of )
					.withDefault( ElasticsearchBackendSettings.Defaults.MAPPING_TYPE_NAME_STRATEGY )
					.build();

	private static final ConfigurationProperty<Boolean> JSON_INTERNAL_VALIDATION =
			ConfigurationProperty.forKey( "json.internal_validation" )
					.asBoolean().withDefault( false ).build();

	static JsonValidation resolveJsonValidation(ConfigurationPropertySource searchPropertySource) {
		return JsonValidation.of( JSON_INTERNAL_VALIDATION.get( searchPropertySource.withFallback(
				AllAwareConfigurationPropertySource.system().withMask( "hibernate.search" ) ) ) );
	}

	private static final ConfigurationProperty<Integer> JSON_MAX_NESTING_DEPTH =
			parserLimit( ElasticsearchBackendSettings.JSON_MAX_NESTING_DEPTH, JsonParsingOptions.DEFAULT.maxDepth() );
	private static final ConfigurationProperty<Integer> JSON_MAX_STRING_LENGTH =
			parserLimit( ElasticsearchBackendSettings.JSON_MAX_STRING_LENGTH, JsonParsingOptions.DEFAULT.maxStringLength() );
	private static final ConfigurationProperty<Integer> JSON_MAX_NAME_LENGTH =
			parserLimit( ElasticsearchBackendSettings.JSON_MAX_NAME_LENGTH, JsonParsingOptions.DEFAULT.maxNameLength() );
	private static final ConfigurationProperty<Integer> JSON_MAX_NUMBER_LENGTH =
			parserLimit( ElasticsearchBackendSettings.JSON_MAX_NUMBER_LENGTH, JsonParsingOptions.DEFAULT.maxNumberLength() );

	private static ConfigurationProperty<Integer> parserLimit(String key, int defaultValue) {
		return ConfigurationProperty.forKey( key ).asIntegerStrictlyPositive().withDefault( defaultValue ).build();
	}

	static JsonParsingOptions resolveJsonParsingOptions(ConfigurationPropertySource backendPropertySource) {
		return new JsonParsingOptions( JSON_MAX_NESTING_DEPTH.get( backendPropertySource ),
				JSON_MAX_STRING_LENGTH.get( backendPropertySource ), JSON_MAX_NAME_LENGTH.get( backendPropertySource ),
				JSON_MAX_NUMBER_LENGTH.get( backendPropertySource ), JsonParsingOptions.DEFAULT.maxInputChunkSize(), 0 );
	}

	@Override
	public BackendImplementor create(EventContext eventContext, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		// All backend parsing shares immutable constraints, including version discovery and response consumers.
		JsonValidation validation = resolveJsonValidation( buildContext.configurationPropertySource() );
		JsonParsing parsing = new JsonParsing( resolveJsonParsingOptions( propertySource ) );
		JsonProvider defaultJsonProvider = JsonProviderHelper.create( logPrettyPrinting, validation, parsing );

		Optional<ElasticsearchVersion> configuredVersion = ElasticsearchLinkImpl.VERSION.get( propertySource );

		BeanResolver beanResolver = buildContext.beanResolver();
		BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder = null;
		BackendThreads threads = null;
		ElasticsearchLinkImpl link = null;
		try {
			threads = new BackendThreads( eventContext.render() );

			clientFactoryHolder = selectClientFactoryReference( propertySource, beanResolver ).resolve( beanResolver );
			ConfigurationLog.INSTANCE.backendClientFactory( clientFactoryHolder, eventContext );

			ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();
			link = new ElasticsearchLinkImpl(
					clientFactoryHolder, threads, defaultJsonProvider, logPrettyPrinting,
					dialectFactory, configuredVersion, createTypeNameMapping( propertySource )
			);
			MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( propertySource, buildContext );

			ElasticsearchModelDialect dialect;
			ElasticsearchVersion version;
			if ( configuredVersion.isPresent()
					&& ElasticsearchDialectFactory.isPreciseEnoughForModelDialect( configuredVersion.get() ) ) {
				version = configuredVersion.get();
			}
			else {
				// We must determine the Elasticsearch version, and thus instantiate the client, right now.
				threads.onStart( propertySource, beanResolver, buildContext.threadPoolProvider() );
				link.onStart( beanResolver, multiTenancyStrategy, propertySource );

				version = link.getElasticsearchVersion();
			}

			dialect = dialectFactory.createModelDialect( version );

			JsonCodecService userFacingCodec = JsonProviderHelper.createUserFacingCodec( validation, parsing );

			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider =
					dialect.createIndexTypeFieldFactoryProvider( userFacingCodec );
			ElasticsearchPropertyMappingValidatorProvider propertyMappingValidatorProvider =
					dialect.createElasticsearchPropertyMappingValidatorProvider();

			return new ElasticsearchBackendImpl(
					buildContext.backendName(),
					eventContext,
					threads, link,
					typeFactoryProvider,
					propertyMappingValidatorProvider,
					userFacingCodec,
					multiTenancyStrategy,
					buildContext.failureHandler(), buildContext.timingSource()
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( BeanHolder::close, clientFactoryHolder )
					.push( ElasticsearchLinkImpl::onStop, link )
					.push( BackendThreads::onStop, threads );
			throw e;
		}
	}

	static BeanReference<? extends ElasticsearchClientFactory> selectClientFactoryReference(
			ConfigurationPropertySource propertySource, BeanResolver beanResolver) {
		Optional<BeanReference<? extends ElasticsearchClientFactory>> configured = CLIENT_FACTORY.get( propertySource );
		if ( configured.isPresent() ) {
			return configured.get();
		}

		Map<String, BeanReference<ElasticsearchClientFactory>> namedFactories =
				beanResolver.namedConfiguredForRole( ElasticsearchClientFactory.class );
		BeanReference<ElasticsearchClientFactory> jdkFactory =
				namedFactories.get( ClientJdkElasticsearchClientFactory.NAME );
		List<BeanReference<ElasticsearchClientFactory>> otherFactories = new ArrayList<>(
				beanResolver.allConfiguredForRole( ElasticsearchClientFactory.class ) );
		otherFactories.remove( jdkFactory );
		if ( otherFactories.size() == 1 ) {
			return otherFactories.get( 0 );
		}
		if ( otherFactories.size() > 1 ) {
			throw ConfigurationLog.INSTANCE.multipleElasticsearchClientFactories( otherFactories );
		}
		return BeanReference.of( ElasticsearchClientFactory.class, ElasticsearchBackendSettings.Defaults.CLIENT_FACTORY );
	}

	private MultiTenancyStrategy getMultiTenancyStrategy(ConfigurationPropertySource propertySource,
			BackendBuildContext buildContext) {
		MultiTenancyStrategyName multiTenancyStrategy = MULTI_TENANCY_STRATEGY.getAndMap(
				propertySource, optionalName -> {
					if ( MultiTenancyStrategyName.NONE.equals( optionalName )
							&& buildContext.multiTenancyEnabled() ) {
						throw ConfigurationLog.INSTANCE.multiTenancyRequiredButExplicitlyDisabledByBackend();
					}
					if ( MultiTenancyStrategyName.DISCRIMINATOR.equals( optionalName )
							&& !buildContext.multiTenancyEnabled() ) {
						throw ConfigurationLog.INSTANCE.multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();
					}
					return optionalName;
				} ).orElseGet( () -> {
					// set dynamic default
					return ( buildContext.multiTenancyEnabled() )
							? MultiTenancyStrategyName.DISCRIMINATOR
							: MultiTenancyStrategyName.NONE;
				} );

		switch ( multiTenancyStrategy ) {
			case NONE:
				return new NoMultiTenancyStrategy();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategy();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported multi-tenancy strategy '%1$s'",
						multiTenancyStrategy
				) );
		}
	}

	private TypeNameMapping createTypeNameMapping(ConfigurationPropertySource propertySource) {
		TypeNameMappingStrategyName strategyName = MAPPING_TYPE_STRATEGY.get( propertySource );

		switch ( strategyName ) {
			case INDEX_NAME:
				return new IndexNameTypeNameMapping();
			case DISCRIMINATOR:
				return new DiscriminatorTypeNameMapping();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported type mapping strategy '%1$s'",
						strategyName
				) );
		}
	}
}
