/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.engine.config.spi.ConfigurationService;

public class HibernateOrmConfigurationServicePropertySource implements
		HibernateOrmAllAwareConfigurationServicePropertySource {

	private static final ConfigurationService.Converter<Object> OBJECT_CONVERTER = value -> value;

	private final ConfigurationService configurationService;

	public HibernateOrmConfigurationServicePropertySource(ConfigurationService configurationService) {
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

	@Override
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
