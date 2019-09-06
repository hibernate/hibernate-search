/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.spi.MappingFinalizationContext;

class MappingFinalizationContextImpl implements MappingFinalizationContext {

	private final ConfigurationPropertySource propertySource;

	MappingFinalizationContextImpl(ConfigurationPropertySource propertySource) {
		this.propertySource = propertySource;
	}

	@Override
	public ConfigurationPropertySource getConfigurationPropertySource() {
		return propertySource;
	}

}
