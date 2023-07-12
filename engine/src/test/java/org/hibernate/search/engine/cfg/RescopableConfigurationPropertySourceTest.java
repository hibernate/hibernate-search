/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource.fromMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.impl.RescopableConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationScope;
import org.hibernate.search.engine.cfg.spi.ConfigurationScopeNamespace;
import org.hibernate.search.engine.cfg.spi.ScopedConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;

public class RescopableConfigurationPropertySourceTest {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void smoke() {
		ScopedConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource.get( "someKey" ) )
				.isPresent()
				.get()
				.isEqualTo( "foo" );

		assertThat( propertySource.get( "someKeyNotThere" ) )
				.isNotPresent();
	}

	@Test
	public void scopeChange() {
		BeanResolver resolver = resolver(
				scope -> {
					if ( scope.matchExact( "global" ) ) {
						return Optional.of( fromMap( singletonMap( "someOtherKey", "global-bar" ) ) );
					}
					if ( scope.matchExact( "backend" ) ) {
						return Optional.of( fromMap( singletonMap( "someOtherKey", "backend-bar" ) ) );
					}

					if ( scope.matchExact( "index", "my-index" ) ) {
						return Optional.of( fromMap( singletonMap( "someOtherKey", "index-my-index-bar" ) ) );
					}

					if ( scope.matchExact( "index" ) ) {
						return Optional.of( fromMap( singletonMap( "someOtherKey", "index-bar" ) ) );
					}


					return Optional.empty();
				}
		);
		ScopedConfigurationPropertySource propertySource = createPropertySource( resolver, "someKey", "foo" );
		assertPair( propertySource, "foo", "global-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"backend"
		);
		assertPair( propertySource, "foo", "backend-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"index"
		);
		assertPair( propertySource, "foo", "index-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"index",
				"my-index"
		);
		assertPair( propertySource, "foo", "index-my-index-bar" );
	}

	@Test
	public void maskAndRescope() {
		BeanResolver resolver = resolver(
				scope -> {
					if ( scope.matchExact( "global" ) ) {
						return Optional
								.of( fromMap( singletonMap( "hibernate.search.someOtherKey", "global-bar" ) ) );
					}
					if ( scope.matchExact( "backend" ) ) {
						return Optional
								.of( fromMap( singletonMap( "hibernate.search.someOtherKey", "backend-bar" ) ) );
					}

					if ( scope.matchExact( "index", "my-index" ) ) {
						return Optional.of(
								fromMap( singletonMap( "hibernate.search.someOtherKey", "index-my-index-bar" ) ) );
					}

					if ( scope.matchExact( "index" ) ) {
						return Optional
								.of( fromMap( singletonMap( "hibernate.search.someOtherKey", "index-bar" ) ) );
					}


					return Optional.empty();
				}
		);
		ScopedConfigurationPropertySource propertySource = createPropertySource( resolver, "hibernate.search.someKey", "foo" )
				.withMask( "hibernate" )
				.withMask( "search" );
		assertPair( propertySource, "foo", "global-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"backend"
		);
		assertPair( propertySource, "foo", "backend-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"index"
		);
		assertPair( propertySource, "foo", "index-bar" );

		propertySource = propertySource.withScope(
				resolver,
				"index",
				"my-index"
		);
		assertPair( propertySource, "foo", "index-my-index-bar" );
	}

	@Test
	public void scopeNoEffect() {
		BeanResolver resolver = resolver(
				scope -> {
					if ( scope.matchExact( "global" ) ) {
						return Optional
								.of( fromMap( singletonMap( "prefix.key", "global-value" ) ) );
					}
					return Optional.empty();
				}
		);
		AllAwareConfigurationPropertySource propertySource = fromMap( singletonMap( "prefix.key", "value" ) );

		ScopedConfigurationPropertySource scoped = ( (ScopedConfigurationPropertySource) propertySource.withMask( "prefix" ) )
				.withScope( resolver, ConfigurationScopeNamespace.GLOBAL );

		assertThat( scoped.get( "key" ) )
				.isPresent()
				.get()
				.isEqualTo( "value" );

		scoped = ( (ScopedConfigurationPropertySource) propertySource.withPrefix( "other" ) )
				.withScope( resolver, ConfigurationScopeNamespace.GLOBAL );

		assertThat( scoped.get( "other.prefix.key" ) )
				.isPresent()
				.get()
				.isEqualTo( "value" );

		scoped = ( (ScopedConfigurationPropertySource) propertySource
				.withFallback( fromMap( singletonMap( "prefix.key", "fallback-value" ) ) ) )
				.withScope( resolver, ConfigurationScopeNamespace.GLOBAL );

		assertThat( scoped.get( "prefix.key" ) )
				.isPresent()
				.get()
				.isEqualTo( "value" );

		scoped = ( (ScopedConfigurationPropertySource) propertySource
				.withOverride( fromMap( singletonMap( "prefix.key", "override-value" ) ) ) )
				.withScope( resolver, ConfigurationScopeNamespace.GLOBAL );

		assertThat( scoped.get( "prefix.key" ) )
				.isPresent()
				.get()
				.isEqualTo( "override-value" );
	}

	@Test
	public void multipleProviders() {
		class ConfigurationProviderA implements ConfigurationProvider {

			@Override
			public Optional<ConfigurationPropertySource> get(ConfigurationScope scope) {
				if ( scope.matchExact( "global" ) ) {
					return Optional
							.of( fromMap( singletonMap( "prefix.key", "global-value-a" ) ) );
				}
				return Optional.empty();
			}

			@Override
			public String toString() {
				return "ConfigurationProviderA";
			}
		}

		class ConfigurationProviderB implements ConfigurationProvider {

			@Override
			public Optional<ConfigurationPropertySource> get(ConfigurationScope scope) {
				if ( scope.matchExact( "global" ) ) {
					return Optional
							.of( fromMap( singletonMap( "prefix.key", "global-value-b" ) ) );
				}
				return Optional.empty();
			}

			@Override
			public String toString() {
				return "ConfigurationProviderB";
			}
		}

		logged.expectEvent(
				Level.WARN,
				"Multiple configuration providers are available for scope 'global'.",
				"They will be taken under consideration in such priority: '[ConfigurationProviderA, ConfigurationProviderB]'."
		);
		ScopedConfigurationPropertySource propertySource = createPropertySource(
				resolver( new ConfigurationProviderB(), new ConfigurationProviderA() ),
				"prefix.key2", "value"
		);

		assertThat( propertySource.get( "prefix.key2" ) )
				.isPresent()
				.get()
				.isEqualTo( "value" );
		assertThat( propertySource.get( "prefix.key" ) )
				.isPresent()
				.get()
				.isEqualTo( "global-value-a" );
	}

	private void assertPair(ScopedConfigurationPropertySource propertySource, String main, String scope) {
		assertThat( propertySource.get( "someKey" ) )
				.isPresent()
				.get()
				.isEqualTo( main );
		assertThat( propertySource.get( "someOtherKey" ) )
				.isPresent()
				.get()
				.isEqualTo( scope );
	}

	private ScopedConfigurationPropertySource createPropertySource(String... properties) {
		return createPropertySource( resolver(), properties );
	}

	private ScopedConfigurationPropertySource createPropertySource(BeanResolver resolver, String... properties) {
		if ( properties.length % 2 != 0 ) {
			throw new IllegalStateException( "Odd number of properties. Properties are a list of (name,value) pairs" );
		}
		Map<String, String> map = new HashMap<>();
		for ( int index = 0; index < properties.length / 2; index += 2 ) {
			map.put( properties[index], properties[index + 1] );
		}
		return new RescopableConfigurationPropertySource(
				resolver,
				fromMap( map )
		);
	}

	private BeanResolver resolver(ConfigurationProvider... providers) {
		return new DummyBeanResolver( providers );
	}

	private static class DummyBeanResolver implements BeanResolver {

		private final List<ConfigurationProvider> providers;

		private DummyBeanResolver(ConfigurationProvider... providers) {
			this.providers = Arrays.asList( providers );
		}

		@Override
		public <T> BeanHolder<T> resolve(Class<T> typeReference, BeanRetrieval retrieval) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference, BeanRetrieval retrieval) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<BeanReference<T>> allConfiguredForRole(Class<T> role) {
			if ( !role.equals( ConfigurationProvider.class ) ) {
				throw new UnsupportedOperationException( "Can only provide " + ConfigurationProvider.class );
			}
			return providers
					.stream()
					.map( role::cast )
					.map( BeanReference::ofInstance )
					.collect( Collectors.toList() );
		}

		@Override
		public <T> Map<String, BeanReference<T>> namedConfiguredForRole(Class<T> role) {
			throw new UnsupportedOperationException();
		}
	}

}
