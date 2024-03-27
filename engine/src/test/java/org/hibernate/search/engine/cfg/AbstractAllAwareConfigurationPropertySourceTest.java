/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public abstract class AbstractAllAwareConfigurationPropertySourceTest {

	@Test
	void get_missing() {
		ConfigurationPropertySource propertySource = createPropertySource( "ignored", "foo" );
		assertThat( propertySource.get( "someKey" ) ).isEmpty();
	}

	@Test
	void get_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( (Optional<Object>) propertySource.get( "someKey" ) ).contains( "foo" );
	}

	@Test
	void resolve_missing() {
		ConfigurationPropertySource propertySource = createPropertySource( "ignored", "foo" );
		assertThat( propertySource.resolve( "someKey" ) ).contains( "someKey" );
	}

	@Test
	void resolve_present() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource.resolve( "someKey" ) ).contains( "someKey" );
	}

	@Test
	void resolveAll_empty() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource( b -> {} );
		assertThat( propertySource.resolveAll( (k, v) -> false ) ).isEmpty();
	}

	@Test
	void resolveAll_keyPredicate() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource( b -> {
			b.accept( "someKey.someSubKey1", "foo" );
			b.accept( "someKey.someSubKey2", "bar" );
			b.accept( "someOtherKey.someSubKey1", "foo" );
			b.accept( "someOtherKey.someSubKey2", "bar" );
		} );
		assertThat( propertySource.resolveAll( (k, v) -> k.startsWith( "someKey." ) ) )
				.containsExactlyInAnyOrder( "someKey.someSubKey1", "someKey.someSubKey2" );
	}

	@Test
	void resolveAll_keyAndValuePredicate() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource( b -> {
			b.accept( "someKey.someSubKey1", "foo" );
			b.accept( "someKey.someSubKey2", "bar" );
			b.accept( "someOtherKey.someSubKey1", "foo" );
			b.accept( "someOtherKey.someSubKey2", "bar" );
		} );
		assertThat( propertySource.resolveAll( (k, v) -> k.startsWith( "someKey." ) && v.equals( "bar" ) ) )
				.containsExactlyInAnyOrder( "someKey.someSubKey2" );
	}

	protected AllAwareConfigurationPropertySource createPropertySource(
			Consumer<BiConsumer<String, String>> contentProducer) {
		Map<String, String> map = new LinkedHashMap<>();
		contentProducer.accept( map::put );
		return createPropertySource( map );
	}

	protected AllAwareConfigurationPropertySource createPropertySource(String key, String value) {
		return createPropertySource( b -> b.accept( key, value ) );
	}

	protected abstract AllAwareConfigurationPropertySource createPropertySource(Map<String, String> content);

}
