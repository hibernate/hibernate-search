/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;

import org.junit.Test;

@SuppressWarnings("unchecked")
public abstract class AbstractAllAwareConfigurationPropertySourceTest {

	@Test
	public void get_missing() {
		ConfigurationPropertySource propertySource = createPropertySource( "ignored", "foo" );
		assertThat( propertySource.get( "someKey" ) ).isEmpty();
	}

	@Test
	public void get_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( (Optional<Object>) propertySource.get( "someKey" ) ).contains( "foo" );
	}

	@Test
	public void resolve_missing() {
		ConfigurationPropertySource propertySource = createPropertySource( "ignored", "foo" );
		assertThat( propertySource.resolve( "someKey" ) ).contains( "someKey" );
	}

	@Test
	public void resolve_present() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource.resolve( "someKey" ) ).contains( "someKey" );
	}

	@Test
	public void resolveAll_empty() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource( b -> {} );
		assertThat( propertySource.resolveAll( (k, v) -> false ) ).isEmpty();
	}

	@Test
	public void resolveAll_keyPredicate() {
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
	public void resolveAll_keyAndValuePredicate() {
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
