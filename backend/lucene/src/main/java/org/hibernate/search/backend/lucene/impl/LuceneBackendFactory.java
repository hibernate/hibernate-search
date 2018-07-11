/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.lucene.cfg.MultiTenancyStrategyConfiguration;
import org.hibernate.search.backend.lucene.cfg.SearchBackendLuceneSettings;
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
import org.hibernate.search.util.EventContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Guillaume Smet
 */
public class LuceneBackendFactory implements BackendFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public BackendImplementor<?> create(String name, BackendBuildContext context, ConfigurationPropertySource propertySource) {
		// TODO be more clever about the type, also supports providing a class
		Optional<String> directoryProviderProperty = DIRECTORY_PROVIDER.get( propertySource );

		EventContext backendContext = EventContexts.fromBackendName( name );

		if ( !directoryProviderProperty.isPresent() ) {
			throw log.undefinedLuceneDirectoryProvider( backendContext );
		}

		String directoryProvider = directoryProviderProperty.get();

		if ( "local_directory".equals( directoryProvider ) ) {
			// TODO GSM: implement the checks properly
			Path rootDirectory = ROOT_DIRECTORY.get( propertySource ).toAbsolutePath();

			MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy( backendContext, propertySource );

			return new LuceneLocalDirectoryBackend( name, rootDirectory, new StubLuceneWorkFactory( multiTenancyStrategy ), multiTenancyStrategy );
		}

		throw log.unrecognizedLuceneDirectoryProvider( directoryProvider, backendContext );
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
}
