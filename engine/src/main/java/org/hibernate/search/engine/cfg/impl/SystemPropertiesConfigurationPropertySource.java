/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.Properties;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public class SystemPropertiesConfigurationPropertySource implements ConfigurationPropertySource {

	private static final SystemPropertiesConfigurationPropertySource INSTANCE = new SystemPropertiesConfigurationPropertySource();

	public static ConfigurationPropertySource get() {
		return INSTANCE;
	}

	private SystemPropertiesConfigurationPropertySource() {
	}

	@Override
	public Optional<?> get(String key) {
		String value = System.getProperty( key );
		return Optional.ofNullable( value );
	}
}
