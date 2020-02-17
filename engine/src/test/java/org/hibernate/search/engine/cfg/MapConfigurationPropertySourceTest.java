/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class MapConfigurationPropertySourceTest extends AbstractAllAwareConfigurationPropertySourceTest {

	@Test
	public void to_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource ).asString()
				.contains( "Map" )
				.contains( "someKey=foo" );
	}

	@Test
	public void get_integer() {
		ConfigurationPropertySource propertySource = ConfigurationPropertySource.fromMap(
				Collections.singletonMap( "someKey", 42 )
		);
		assertThat( (Optional<Object>) propertySource.get( "someKey" ) ).contains( 42 );
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(String key, String value) {
		Map<String, Object> map = Collections.singletonMap( key, value );
		return ConfigurationPropertySource.fromMap( map );
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(String key, String value,
			String key2, String value2) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( key, value );
		map.put( key2, value2 );
		return ConfigurationPropertySource.fromMap( map );
	}
}
