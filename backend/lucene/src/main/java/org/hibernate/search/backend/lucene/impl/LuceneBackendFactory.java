/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.LuceneAnalysisDefinitionContainerContextImpl;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.cfg.MultiTenancyStrategyConfiguration;
import org.hibernate.search.backend.lucene.cfg.SearchBackendLuceneSettings;
import org.hibernate.search.backend.lucene.index.impl.DirectoryProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.DiscriminatorMultiTenancyStrategyImpl;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.multitenancy.impl.NoMultiTenancyStrategyImpl;
import org.hibernate.search.backend.lucene.work.impl.StubLuceneWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.util.Version;


/**
 * @author Guillaume Smet
 */
public class LuceneBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Optional<Version>> LUCENE_VERSION =
			ConfigurationProperty.forKey( SearchBackendLuceneSettings.LUCENE_VERSION )
					.as( Version.class, LuceneBackendFactory::parseLuceneVersion )
					.build();

	private static final ConfigurationProperty<Optional<String>> DIRECTORY_PROVIDER =
			ConfigurationProperty.forKey( SearchBackendLuceneSettings.LUCENE_DIRECTORY_PROVIDER )
					.asString()
					.build();

	private static final ConfigurationProperty<Path> ROOT_DIRECTORY =
			ConfigurationProperty.forKey( SearchBackendLuceneSettings.LUCENE_ROOT_DIRECTORY )
					.as( Path.class, Paths::get )
					.withDefault( () -> Paths.get( "." ) )
					.build();

	private static final ConfigurationProperty<MultiTenancyStrategyConfiguration> MULTI_TENANCY_STRATEGY =
			ConfigurationProperty.forKey( SearchBackendLuceneSettings.MULTI_TENANCY_STRATEGY )
					.as( MultiTenancyStrategyConfiguration.class, MultiTenancyStrategyConfiguration::fromExternalRepresentation )
					.withDefault( SearchBackendLuceneSettings.Defaults.MULTI_TENANCY_STRATEGY )
					.build();

	@Override
	public BackendImplementor<?> create(String name, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		EventContext backendContext = EventContexts.fromBackendName( name );

		Version luceneVersion = getLuceneVersion( backendContext, propertySource );

		DirectoryProvider directoryProvider = getDirectoryProvider( backendContext, propertySource );

		MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( backendContext, propertySource );

		LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry = getAnalysisDefinitionRegistry(
				backendContext, buildContext, propertySource, luceneVersion
		);

		return new LuceneBackendImpl(
				name,
				directoryProvider,
				new StubLuceneWorkFactory( multiTenancyStrategy ),
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
			Version latestVersion = SearchBackendLuceneSettings.Defaults.LUCENE_VERSION;
			log.recommendConfiguringLuceneVersion( latestVersion, backendContext );
			luceneVersion = latestVersion;
		}
		return luceneVersion;
	}

	private DirectoryProvider getDirectoryProvider(EventContext backendContext, ConfigurationPropertySource propertySource) {
		// TODO be more clever about the type, also supports providing a class
		Optional<String> directoryProviderProperty = DIRECTORY_PROVIDER.get( propertySource );

		if ( !directoryProviderProperty.isPresent() ) {
			throw log.undefinedLuceneDirectoryProvider( backendContext );
		}

		String directoryProviderString = directoryProviderProperty.get();

		if ( "local_directory".equals( directoryProviderString ) ) {
			// TODO GSM: implement the checks properly
			Path rootDirectory = ROOT_DIRECTORY.get( propertySource ).toAbsolutePath();

			initializeRootDirectory( rootDirectory, backendContext );
			return new MMapDirectoryProvider( backendContext, rootDirectory );
		}

		throw log.unrecognizedLuceneDirectoryProvider( directoryProviderString, backendContext );
	}

	private MultiTenancyStrategy getMultiTenancyStrategy(EventContext backendContext, ConfigurationPropertySource propertySource) {
		MultiTenancyStrategyConfiguration multiTenancyStrategyConfiguration = MULTI_TENANCY_STRATEGY.get( propertySource );

		switch ( multiTenancyStrategyConfiguration ) {
			case NONE:
				return new NoMultiTenancyStrategyImpl();
			case DISCRIMINATOR:
				return new DiscriminatorMultiTenancyStrategyImpl();
			default:
				throw new AssertionFailure( String.format(
						Locale.ROOT, "Unsupported multi-tenancy strategy '%1$s'. %2$s",
						multiTenancyStrategyConfiguration,
						backendContext.render()
				) );
		}
	}

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry(EventContext backendContext,
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource,
			Version luceneVersion) {
		try {
			// Apply the user-provided analysis configurer if necessary
			final BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
			ConfigurationProperty<Optional<LuceneAnalysisConfigurer>> analysisConfigurerProperty =
					ConfigurationProperty.forKey( SearchBackendLuceneSettings.ANALYSIS_CONFIGURER )
							.as(
									LuceneAnalysisConfigurer.class,
									reference -> beanProvider
											.getBean( reference, LuceneAnalysisConfigurer.class )
							)
							.build();
			return analysisConfigurerProperty.get( propertySource )
					.map( configurer -> {
						LuceneAnalysisComponentFactory analysisComponentFactory = new LuceneAnalysisComponentFactory(
								luceneVersion,
								buildContext.getServiceManager().getClassResolver(),
								buildContext.getServiceManager().getResourceResolver()
						);
						LuceneAnalysisDefinitionContainerContextImpl collector
								= new LuceneAnalysisDefinitionContainerContextImpl( analysisComponentFactory );
						configurer.configure( collector );
						return new LuceneAnalysisDefinitionRegistry( collector );
					} )
					// Otherwise just use an empty registry
					.orElseGet( LuceneAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), backendContext, e );
		}
	}

	private void initializeRootDirectory(Path rootDirectory, EventContext eventContext) {
		if ( Files.exists( rootDirectory ) ) {
			if ( !Files.isDirectory( rootDirectory ) || !Files.isWritable( rootDirectory ) ) {
				throw log.localDirectoryBackendRootDirectoryNotWritableDirectory( rootDirectory, eventContext );
			}
		}
		else {
			try {
				Files.createDirectories( rootDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateRootDirectoryForLocalDirectoryBackend( rootDirectory, eventContext, e );
			}
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
