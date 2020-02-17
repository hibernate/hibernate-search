/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

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
	public void resolveAll_missing() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource( "ignored", "foo" );
		assertThat( propertySource.resolveAll( "someKey" ) ).isEmpty();
	}

	@Test
	public void resolveAll_present() {
		AllAwareConfigurationPropertySource propertySource = createPropertySource(
				"someKey.someSubKey1", "foo",
				"someKey.someSubKey2", "foo"
		);
		assertThat( propertySource.resolveAll( "someKey." ) )
				.containsExactlyInAnyOrder( "someKey.someSubKey1", "someKey.someSubKey2" );
	}

	protected abstract AllAwareConfigurationPropertySource createPropertySource(String key, String value);

	protected abstract AllAwareConfigurationPropertySource createPropertySource(String key, String value,
			String key2, String value2);
}
