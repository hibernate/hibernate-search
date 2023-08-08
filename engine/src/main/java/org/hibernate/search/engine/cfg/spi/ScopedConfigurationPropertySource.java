/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public final class ScopedConfigurationPropertySource implements ConfigurationPropertySource {

	public static ScopedConfigurationPropertySource wrap(BeanResolver resolver,
			ConfigurationPropertySource configurationPropertySource) {
		return new ScopedConfigurationPropertySource( resolver, configurationPropertySource );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final ConfigurationScope scope;
	private final ConfigurationPropertySource delegate;
	private final ConfigurationPropertySource delegateNoScope;
	private final Map<ConfigurationScope,
			List<Function<ConfigurationPropertySource, ConfigurationPropertySource>>> scopeModifiers;

	private ScopedConfigurationPropertySource(BeanResolver beanResolver, ConfigurationPropertySource delegate) {
		this(
				beanResolver,
				ConfigurationScope.GLOBAL,
				delegate.withFallback( scopeFallback( beanResolver, ConfigurationScope.GLOBAL, Map.of() ) ),
				delegate,
				Map.of()
		);
	}

	private ScopedConfigurationPropertySource(
			BeanResolver beanResolver,
			ConfigurationScope scope,
			ConfigurationPropertySource delegate,
			ConfigurationPropertySource delegateNoScope,
			Map<ConfigurationScope, List<Function<ConfigurationPropertySource, ConfigurationPropertySource>>> scopeModifiers) {
		this.scope = scope;
		this.beanResolver = beanResolver;
		this.delegate = delegate;
		this.delegateNoScope = delegateNoScope;
		this.scopeModifiers = new LinkedHashMap<>();
		for ( Map.Entry<ConfigurationScope,
				List<Function<ConfigurationPropertySource, ConfigurationPropertySource>>> entry : scopeModifiers.entrySet() ) {
			this.scopeModifiers.put( entry.getKey(), new ArrayList<>( entry.getValue() ) );
		}
		this.scopeModifiers.computeIfAbsent( scope, ignored -> new ArrayList<>() );
	}

	private ScopedConfigurationPropertySource(
			BeanResolver beanResolver,
			ConfigurationScope scope,
			ConfigurationPropertySource delegate,
			ConfigurationPropertySource delegateNoScope,
			Map<ConfigurationScope, List<Function<ConfigurationPropertySource, ConfigurationPropertySource>>> scopeModifiers,
			Function<ConfigurationPropertySource, ConfigurationPropertySource> modifier) {
		this( beanResolver, scope, delegate, delegateNoScope, scopeModifiers );
		this.scopeModifiers.get( scope ).add( modifier );
	}

	public ScopedConfigurationPropertySource withScope(String namespace) {
		return withScope( namespace, null );
	}

	public ScopedConfigurationPropertySource withScope(String namespace, String name) {
		ConfigurationScope reduced = scope.reduce( namespace, name );
		return new ScopedConfigurationPropertySource(
				beanResolver,
				reduced,
				delegateNoScope.withFallback( scopeFallback( beanResolver, reduced, scopeModifiers ) ),
				delegateNoScope,
				scopeModifiers
		);
	}

	@Override
	public Optional<?> get(String key) {
		return delegate.get( key );
	}

	@Override
	public Optional<String> resolve(String key) {
		return delegate.resolve( key );
	}

	@Override
	public ScopedConfigurationPropertySource withPrefix(String prefix) {
		return new ScopedConfigurationPropertySource(
				beanResolver,
				scope,
				delegate.withPrefix( prefix ),
				delegateNoScope.withPrefix( prefix ),
				scopeModifiers,
				source -> source.withPrefix( prefix )
		);
	}

	@Override
	public ScopedConfigurationPropertySource withMask(String mask) {
		return new ScopedConfigurationPropertySource(
				beanResolver,
				scope,
				delegate.withMask( mask ),
				delegateNoScope.withMask( mask ),
				scopeModifiers,
				source -> source.withMask( mask )
		);
	}

	@Override
	public ScopedConfigurationPropertySource withFallback(ConfigurationPropertySource fallback) {
		return new ScopedConfigurationPropertySource(
				beanResolver,
				scope,
				delegate.withFallback( fallback ),
				delegateNoScope.withFallback( fallback ),
				scopeModifiers,
				source -> source.withFallback( fallback )
		);
	}

	@Override
	public ScopedConfigurationPropertySource withOverride(ConfigurationPropertySource override) {
		return new ScopedConfigurationPropertySource(
				beanResolver,
				scope,
				delegate.withOverride( override ),
				delegateNoScope.withOverride( override ),
				scopeModifiers,
				source -> source.withOverride( override )
		);
	}

	private static ConfigurationPropertySource scopeFallback(BeanResolver beanResolver, ConfigurationScope scope,
			Map<ConfigurationScope, List<Function<ConfigurationPropertySource, ConfigurationPropertySource>>> scopeModifiers) {
		try ( BeanHolder<List<ConfigurationProvider>> configurationProviderHolders =
				beanResolver.resolve( beanResolver.allConfiguredForRole( ConfigurationProvider.class ) ) ) {
			List<ConfigurationProvider> configurationProviders = configurationProviderHolders.get()
					.stream().sorted().collect( Collectors.toList() );

			if ( configurationProviders.size() > 1 ) {
				log.multipleConfigurationProvidersAvailable( scope.toString(), configurationProviders );
			}

			ConfigurationPropertySource fallback = ConfigurationPropertySource.empty();
			ConfigurationScope scopeIterator = scope;
			do {
				for ( ConfigurationProvider provider : configurationProviders ) {
					fallback = provider.get( scopeIterator )
							.map( source -> transform( source, scopeModifiers.getOrDefault( scope, List.of() ) ) )
							.map( fallback::withFallback )
							.orElse( fallback );
				}
				scopeIterator = scopeIterator.parent();
			}
			while ( scopeIterator != null );

			return fallback;
		}
	}

	private static ConfigurationPropertySource transform(ConfigurationPropertySource source,
			List<Function<ConfigurationPropertySource, ConfigurationPropertySource>> transformers) {
		for ( Function<ConfigurationPropertySource, ConfigurationPropertySource> transformer : transformers ) {
			source = transformer.apply( source );
		}
		return source;
	}
}
