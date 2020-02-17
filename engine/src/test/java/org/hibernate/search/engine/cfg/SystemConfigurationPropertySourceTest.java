/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.junit.After;
import org.junit.Test;

public class SystemConfigurationPropertySourceTest extends AbstractAllAwareConfigurationPropertySourceTest {
	private final List<String> toClear = new ArrayList<>();

	@Test
	public void to_string() {
		ConfigurationPropertySource propertySource = createPropertySource( "someKey", "foo" );
		assertThat( propertySource ).asString().contains( "System" );
	}

	@After
	public void clearSystemProperties() {
		for ( String key : toClear ) {
			System.clearProperty( key );
		}
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(String key, String value) {
		clearSystemProperties();
		toClear.add( key );
		System.setProperty( key, value );
		return ConfigurationPropertySource.system();
	}

	@Override
	protected AllAwareConfigurationPropertySource createPropertySource(String key, String value,
			String key2, String value2) {
		clearSystemProperties();
		toClear.add( key );
		toClear.add( key2 );
		System.setProperty( key, value );
		System.setProperty( key2, value2 );
		return ConfigurationPropertySource.system();
	}
}
