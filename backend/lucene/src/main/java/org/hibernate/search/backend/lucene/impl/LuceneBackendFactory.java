/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.LuceneAnalysisConfigurationContextImpl;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneDefaultAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurationContext;
import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurer;
import org.hibernate.search.backend.lucene.cache.impl.LuceneQueryCachingContext;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.backend.lucene.multitenancy.impl.DiscriminatorMultiTenancyStrategy;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.multitenancy.impl.NoMultiTenancyStrategy;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
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
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.util.Version;

public class LuceneBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Optional<Version>> LUCENE_VERSION =
			ConfigurationProperty.forKey( LuceneBackendSettings.LUCENE_VERSION )
					.as( Version.class, LuceneBackendFactory::parseLuceneVersion )
					.build();

	private static final OptionalConfigurationProperty<MultiTenancyStrategyName> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( LuceneBackendSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyName.class, MultiTenancyStrategyName::of )
					.build();

	private static final OptionalConfigurationProperty<
			List<BeanReference<? extends LuceneAnalysisConfigurer>>> ANALYSIS_CONFIGURER =
					ConfigurationProperty.forKey( LuceneBackendSettings.ANALYSIS_CONFIGURER )
							.asBeanReference( LuceneAnalysisConfigurer.class )
							.multivalued()
							.build();

	private static final OptionalConfigurationProperty<
			List<BeanReference<? extends QueryCachingConfigurer>>> QUERY_CACHING_CONFIGURER =
					ConfigurationProperty.forKey( LuceneBackendSettings.QUERY_CACHING_CONFIGURER )
							.asBeanReference( QueryCachingConfigurer.class )
							.multivalued()
							.build();

	@Override
	public BackendImplementor create(EventContext eventContext, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		BackendThreads backendThreads = null;

		try {
			backendThreads = new BackendThreads( eventContext.render() );

			Version luceneVersion = getLuceneVersion( eventContext, propertySource );

			MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( propertySource, buildContext );

			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry = getAnalysisDefinitionRegistry(
					buildContext, propertySource, luceneVersion
			);

			LuceneQueryCachingContext cachingContext = new LuceneQueryCachingContext( luceneVersion );

			configureQueryCache( buildContext, propertySource, cachingContext );

			return new LuceneBackendImpl(
					buildContext.backendName(),
					eventContext,
					backendThreads,
					new LuceneWorkFactory( multiTenancyStrategy ),
					analysisDefinitionRegistry,
					cachingContext,
					multiTenancyStrategy,
					buildContext.timingSource(),
					buildContext.failureHandler()
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( BackendThreads::onStop, backendThreads );
			throw e;
		}
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

	private MultiTenancyStrategy getMultiTenancyStrategy(ConfigurationPropertySource propertySource,
			BackendBuildContext buildContext) {
		MultiTenancyStrategyName multiTenancyStrategy = MULTI_TENANCY_STRATEGY.getAndMap(
				propertySource, optionalName -> {
					if ( MultiTenancyStrategyName.NONE.equals( optionalName )
							&& buildContext.multiTenancyEnabled() ) {
						throw log.multiTenancyRequiredButExplicitlyDisabledByBackend();
					}
					if ( MultiTenancyStrategyName.DISCRIMINATOR.equals( optionalName )
							&& !buildContext.multiTenancyEnabled() ) {
						throw log.multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();
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

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource,
			Version luceneVersion) {
		try {
			LuceneAnalysisComponentFactory analysisComponentFactory = new LuceneAnalysisComponentFactory(
					luceneVersion,
					buildContext.classResolver(),
					buildContext.resourceResolver()
			);
			LuceneAnalysisConfigurationContextImpl collector =
					new LuceneAnalysisConfigurationContextImpl( analysisComponentFactory );
			// Add default definitions first, so that they can be overridden
			LuceneDefaultAnalysisConfigurer.INSTANCE.configure( collector );
			// Apply the user-provided analysis configurers if necessary
			final BeanResolver beanResolver = buildContext.beanResolver();
			ANALYSIS_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
					.ifPresent( holder -> {
						try ( BeanHolder<List<LuceneAnalysisConfigurer>> configurerHolder = holder ) {
							for ( LuceneAnalysisConfigurer configurer : configurerHolder.get() ) {
								configurer.configure( collector );
							}
						}
					} );
			return new LuceneAnalysisDefinitionRegistry( collector );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), e );
		}
	}

	private void configureQueryCache(
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource,
			QueryCachingConfigurationContext context) {
		try {
			final BeanResolver beanResolver = buildContext.beanResolver();
			try ( BeanHolder<List<QueryCachingConfigurer>> implicitConfigurersHolder =
					beanResolver.resolve( beanResolver.allConfiguredForRole( QueryCachingConfigurer.class ) ) ) {
				for ( QueryCachingConfigurer configurer : implicitConfigurersHolder.get() ) {
					configurer.configure( context );
				}
			}

			// Apply the user-provided query cache configurers if necessary
			QUERY_CACHING_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
					.ifPresent( holder -> {
						try ( BeanHolder<List<QueryCachingConfigurer>> configurerHolder = holder ) {
							for ( QueryCachingConfigurer configurer : configurerHolder.get() ) {
								configurer.configure( context );
							}
						}
					} );
		}
		catch (Exception e) {
			throw log.unableToApplyQueryCacheConfiguration( e.getMessage(), e );
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
