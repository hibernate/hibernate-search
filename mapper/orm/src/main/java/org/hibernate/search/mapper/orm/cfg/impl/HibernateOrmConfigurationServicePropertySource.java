/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

class HibernateOrmConfigurationServicePropertySource implements ConfigurationPropertySource {

	private static final ConfigurationService.Converter<Object> OBJECT_CONVERTER = value -> value;

	private final ConfigurationService configurationService;

	HibernateOrmConfigurationServicePropertySource(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public Optional<?> get(String key) {
		return Optional.ofNullable( configurationService.getSetting( key, OBJECT_CONVERTER ) );
	}

	@Override
	public Optional<String> resolve(String key) {
		return Optional.of( key );
	}

	public Map<?, ?> getAllRawProperties() {
		return configurationService.getSettings();
	}

	public Set<String> resolveAll(String prefix) {
		Set<String> hibernateSearchPropertyKeys = new HashSet<>();
		for ( Object key : configurationService.getSettings().keySet() ) {
			if ( key instanceof String ) {
				String stringKey = (String) key;
				if ( stringKey.startsWith( prefix ) ) {
					hibernateSearchPropertyKeys.add( stringKey );
				}
			}
		}
		return hibernateSearchPropertyKeys;
	}
}
