/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProviderHelper;
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
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
			ConfigurationProperty.forKey( ElasticsearchBackendSpiSettings.CLIENT_FACTORY )
					.asBeanReference( ElasticsearchClientFactory.class )
					.build();

	private static final ConfigurationProperty<TypeNameMappingStrategyName> MAPPING_TYPE_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY )
					.as( TypeNameMappingStrategyName.class, TypeNameMappingStrategyName::of )
					.withDefault( ElasticsearchBackendSettings.Defaults.MAPPING_TYPE_NAME_STRATEGY )
					.build();

	@Override
	public BackendImplementor create(EventContext eventContext, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		boolean logPrettyPrinting = LOG_JSON_PRETTY_PRINTING.get( propertySource );
		/*
		 * The Elasticsearch client only converts JsonObjects to String and
		 * vice-versa, it doesn't need a Gson instance that was specially
		 * configured for a particular Elasticsearch version.
		 */
		GsonProvider defaultGsonProvider = GsonProviderHelper.create( GsonBuilder::new, logPrettyPrinting );

		Optional<ElasticsearchVersion> configuredVersion = ElasticsearchLinkImpl.VERSION.get( propertySource );

		BeanResolver beanResolver = buildContext.beanResolver();
		BeanHolder<? extends ElasticsearchClientFactory> clientFactoryHolder = null;
		BackendThreads threads = null;
		ElasticsearchLinkImpl link = null;
		try {
			threads = new BackendThreads( eventContext.render() );

			// First, let's see if the factory was configured explicitly:
			Optional<BeanHolder<? extends ElasticsearchClientFactory>> customClientFactoryHolderOptional =
					CLIENT_FACTORY.getAndMap( propertySource, beanResolver::resolve );
			if ( customClientFactoryHolderOptional.isPresent() ) {
				clientFactoryHolder = customClientFactoryHolderOptional.get();
			}
			else {
				// otherwise let's find all client factories and pick
				List<BeanReference<ElasticsearchClientFactory>> clientFactoryReferences =
						beanResolver.allConfiguredForRole( ElasticsearchClientFactory.class );
				if ( clientFactoryReferences.isEmpty() ) {
					throw ConfigurationLog.INSTANCE.backendClientFactoryNotConfigured( eventContext );
				}
				// if there's just one -- use it:
				else if ( clientFactoryReferences.size() == 1 ) {
					clientFactoryHolder = clientFactoryReferences.get( 0 ).resolve( beanResolver );
				}
				// if there are 2 of them, maybe one is the "default" one, if so -- use the other one
				else if ( clientFactoryReferences.size() == 2 ) {
					var defaultFactoryReference = beanResolver.namedConfiguredForRole( ElasticsearchClientFactory.class )
							.get( ElasticsearchClientFactory.DEFAULT_BEAN_NAME );

					var first = clientFactoryReferences.get( 0 );
					var second = clientFactoryReferences.get( 1 );
					if ( first == defaultFactoryReference ) {
						clientFactoryHolder = second.resolve( beanResolver );
					}
					else if ( second == defaultFactoryReference ) {
						clientFactoryHolder = first.resolve( beanResolver );
					}
				}
				if ( clientFactoryHolder == null ) {
					throw ConfigurationLog.INSTANCE.backendClientFactoryMultipleConfigured(
							clientFactoryReferences.stream().map( ref -> ref.resolve( beanResolver ) ).toList(),
							eventContext
					);
				}
			}
			ConfigurationLog.INSTANCE.backendClientFactory( clientFactoryHolder, eventContext );

			ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();
			link = new ElasticsearchLinkImpl(
					clientFactoryHolder, threads, defaultGsonProvider, logPrettyPrinting,
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

			Gson userFacingGson = new GsonBuilder().setPrettyPrinting().create();

			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider =
					dialect.createIndexTypeFieldFactoryProvider( userFacingGson );
			ElasticsearchPropertyMappingValidatorProvider propertyMappingValidatorProvider =
					dialect.createElasticsearchPropertyMappingValidatorProvider();

			return new ElasticsearchBackendImpl(
					buildContext.backendName(),
					eventContext,
					threads, link,
					typeFactoryProvider,
					propertyMappingValidatorProvider,
					userFacingGson,
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
