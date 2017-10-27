/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.util.Optional;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

public class HibernateOrmConfigurationPropertySource implements ConfigurationPropertySource {

	private static final ConfigurationService.Converter<Object> OBJECT_CONVERTER = value -> value;

	private final ConfigurationService configurationService;

	public HibernateOrmConfigurationPropertySource(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public Optional<?> get(String key) {
		return Optional.ofNullable( configurationService.getSetting( key, OBJECT_CONVERTER ) );
	}
}
