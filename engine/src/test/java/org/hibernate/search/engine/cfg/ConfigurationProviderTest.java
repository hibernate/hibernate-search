/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceScopeUtils.backend;
import static org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceScopeUtils.fallback;
import static org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceScopeUtils.global;
import static org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceScopeUtils.index;
import static org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource.fromMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationScope;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class ConfigurationProviderTest {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void smoke() {
		ConfigurationPropertySource propertySource = fromMap( asMap( "someKey", "foo" ) )
				.withFallback( fallback( resolver(), global() ) );
		assertThat( propertySource.get( "someKey" ) )
				.isPresent()
				.get()
				.isEqualTo( "foo" );

		assertThat( propertySource.get( "someKeyNotThere" ) )
				.isNotPresent();
	}

	@Test
	void scopeChange() {
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
		ConfigurationPropertySource propertySource = fromMap( asMap( "someKey", "foo" ) )
				.withFallback( fallback( resolver, global() ) );

		assertPair( propertySource, "foo", "global-bar" );

		propertySource = propertySource.withFallback( fallback( resolver, backend() ) );
		assertPair( propertySource, "foo", "global-bar" );

		propertySource = propertySource.withFallback( fallback( resolver, index( null, "my-index" ) ) );
		assertPair( propertySource, "foo", "global-bar" );
	}

	@Test
	void maskAndFallbackForScope() {
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
		ConfigurationPropertySource propertySource = fromMap( asMap( "hibernate.search.someKey", "foo" ) )
				.withMask( "hibernate" )
				.withMask( "search" )
				.withFallback( fallback( resolver, global() ) );

		assertPair( propertySource, "foo", "global-bar" );

		propertySource = propertySource.withFallback( fallback( resolver(), backend() ) );
		assertPair( propertySource, "foo", "global-bar" );
	}

	@Test
	void multipleProviders() {
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
			public int priority() {
				return 2;
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
			public int priority() {
				return 1;
			}

			@Override
			public String toString() {
				return "ConfigurationProviderB";
			}
		}

		logged.expectEvent(
				Level.DEBUG,
				"Multiple configuration providers are available for scope 'global'.",
				"They will be taken under consideration in the following order: '[ConfigurationProviderB, ConfigurationProviderA]'."
		);
		ConfigurationPropertySource propertySource = fromMap( asMap(
				"prefix.key2", "value"
		) ).withFallback(
				fallback( resolver( new ConfigurationProviderB(), new ConfigurationProviderA() ), global() )
		);

		assertThat( propertySource.get( "prefix.key2" ) )
				.isPresent()
				.get()
				.isEqualTo( "value" );
		assertThat( propertySource.get( "prefix.key" ) )
				.isPresent()
				.get()
				.isEqualTo( "global-value-b" );
	}

	@Test
	void almostRealisticScenario() {
		BeanResolver resolver = resolver(
				scope -> {
					if ( scope.matchExact( "global" ) ) {
						return Optional.of( fromMap( asMap(
								"hibernate.search.backend.my-backend.index.my-index.override-setting", "global",
								"hibernate.search.global-specific-setting", "global"
						) ) );
					}
					if ( scope.matchExact( "backend", "my-backend" ) ) {
						return Optional.of( fromMap( asMap(
								"index.my-index.override-setting", "my-backend",
								"my-backend-specific-setting", "my-backend"
						) ) );
					}

					if ( scope.matchExact( "backend" ) ) {
						return Optional.of( fromMap( asMap(
								"my-backend.index.my-index.override-setting", "backend",
								"backend-specific-setting", "backend"
						) ) );
					}

					if ( scope.matchExact( "index", "my-index" ) ) {
						return Optional.of( fromMap( asMap(
								"override-setting", "my-index",
								"my-index-specific-setting", "my-index"
						) ) );
					}

					if ( scope.matchExact( "index" ) ) {
						return Optional.of( fromMap( asMap(
								"my-index.override", "index",
								"index-specific-setting", "index"
						) ) );
					}


					return Optional.empty();
				}
		);

		ConfigurationPropertySource userSource = fromMap(
				asMap( "hibernate.search.backend.my-backend.index.my-index.override-setting", "user" ) );

		assertThat( userSource.get( "hibernate.search.backend.my-backend.index.my-index.override-setting" ) )
				.isPresent()
				.get()
				.isEqualTo( "user" );

		ConfigurationPropertySource source = userSource
				.withFallback( fallback( resolver, global() ) )
				.withMask( "hibernate.search" );

		assertThat( source.get( "backend.my-backend.index.my-index.override-setting" ) )
				.isPresent()
				.get()
				.isEqualTo( "user" );

		source = source.withMask( "backend" )
				.withFallback( fallback( resolver, backend() ) )
				.withMask( "my-backend" )
				.withFallback( fallback( resolver, backend( "my-backend" ) ) );
		assertThat( source.get( "index.my-index.override-setting" ) )
				.isPresent()
				.get()
				.isEqualTo( "user" );

		source = source.withMask( "index" )
				.withMask( "my-index" )
				.withFallback( fallback( resolver, index( "my-backend", "my-index" ) ) );
		assertThat( source.get( "override-setting" ) )
				.isPresent()
				.get()
				.isEqualTo( "user" );
	}

	private Map<String, String> asMap(String... properties) {
		if ( properties.length % 2 != 0 ) {
			throw new IllegalStateException( "Odd number of properties. Properties are a list of (name,value) pairs" );
		}
		Map<String, String> map = new HashMap<>();
		for ( int index = 0; index < properties.length / 2; index += 2 ) {
			map.put( properties[index], properties[index + 1] );
		}
		return map;
	}

	private void assertPair(ConfigurationPropertySource propertySource, String main, String scope) {
		assertThat( propertySource.get( "someKey" ) )
				.isPresent()
				.get()
				.isEqualTo( main );
		assertThat( propertySource.get( "someOtherKey" ) )
				.isPresent()
				.get()
				.isEqualTo( scope );
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
