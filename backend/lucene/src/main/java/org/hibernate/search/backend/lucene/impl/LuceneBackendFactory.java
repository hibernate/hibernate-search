/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.LuceneAnalysisConfigurationContextImpl;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.cfg.MultiTenancyStrategyName;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryProviderInitializationContextImpl;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactoryImpl;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.util.Version;



public class LuceneBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Optional<Version>> LUCENE_VERSION =
			ConfigurationProperty.forKey( LuceneBackendSettings.LUCENE_VERSION )
					.as( Version.class, LuceneBackendFactory::parseLuceneVersion )
					.build();

	private static final ConfigurationProperty<MultiTenancyStrategyName> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( LuceneBackendSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyName.class, MultiTenancyStrategyName::of )
					.withDefault( LuceneBackendSettings.Defaults.MULTI_TENANCY_STRATEGY )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends LuceneAnalysisConfigurer>> ANALYSIS_CONFIGURER =
			ConfigurationProperty.forKey( LuceneBackendSettings.ANALYSIS_CONFIGURER )
					.asBeanReference( LuceneAnalysisConfigurer.class )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		EventContext backendContext = EventContexts.fromBackendName( name );

		Version luceneVersion = getLuceneVersion( backendContext, propertySource );

		BeanHolder<? extends DirectoryProvider> directoryProviderHolder =
				getDirectoryProvider( backendContext, buildContext, propertySource );

		MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( propertySource );

		LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry = getAnalysisDefinitionRegistry(
				buildContext, propertySource, luceneVersion
		);

		return new LuceneBackendImpl(
				name,
				directoryProviderHolder,
				new LuceneWorkFactoryImpl( multiTenancyStrategy ),
				analysisDefinitionRegistry,
				multiTenancyStrategy
		);
	}

	private Version getLuceneVersion(EventContext backendContext, ConfigurationPropertySource propertySource) {
		Version luceneVersion;
		Optional<Version> luceneVersionOptional = LUCENE_VERSION.get( propertySource );
		if ( luceneVersionOptional.isPresent() ) {
			luceneVersion = luceneVersionOptional.get();
			if ( log.isDebugEnabled() ) {
				log.debug( "Setting Lucene compatibility to Version " + luceneVersion );
			}
		}
		else {
			Version latestVersion = LuceneBackendSettings.Defaults.LUCENE_VERSION;
			log.recommendConfiguringLuceneVersion(
					LUCENE_VERSION.resolveOrRaw( propertySource ),
					latestVersion,
					backendContext
			);
			luceneVersion = latestVersion;
		}
		return luceneVersion;
	}

	private BeanHolder<? extends DirectoryProvider> getDirectoryProvider(EventContext backendContext,
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		DirectoryProviderInitializationContextImpl initializationContext = new DirectoryProviderInitializationContextImpl(
				backendContext,
				buildContext.getBeanResolver(),
				propertySource.withMask( "directory" )
		);
		return initializationContext.createDirectoryProvider();
	}

	private MultiTenancyStrategy getMultiTenancyStrategy(ConfigurationPropertySource propertySource) {
		MultiTenancyStrategyName multiTenancyStrategyName = MULTI_TENANCY_STRATEGY.get( propertySource );

		switch ( multiTenancyStrategyName ) {
			case NONE:
				return new NoMultiTenancyStrategy();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategy();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported multi-tenancy strategy '%1$s'.",
						multiTenancyStrategyName
				) );
		}
	}

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource,
			Version luceneVersion) {
		try {
			// Apply the user-provided analysis configurer if necessary
			final BeanResolver beanResolver = buildContext.getBeanResolver();
			return ANALYSIS_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
					.map( holder -> {
						try ( BeanHolder<? extends LuceneAnalysisConfigurer> configurerHolder = holder ) {
							LuceneAnalysisComponentFactory analysisComponentFactory = new LuceneAnalysisComponentFactory(
									luceneVersion,
									buildContext.getClassResolver(),
									buildContext.getResourceResolver()
							);
							LuceneAnalysisConfigurationContextImpl collector =
									new LuceneAnalysisConfigurationContextImpl( analysisComponentFactory );
							configurerHolder.get().configure( collector );
							return new LuceneAnalysisDefinitionRegistry( collector );
						}
					} )
					// Otherwise just use an empty registry
					.orElseGet( LuceneAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), e );
		}
	}

	private static Version parseLuceneVersion(String versionString) {
		try {
			return Version.parseLeniently( versionString );
		}
		catch (IllegalArgumentException | ParseException e) {
			throw log.illegalLuceneVersionFormat( versionString, e.getMessage(), e );
		}
	}
}
