/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

/**
 * The context passed to the mapper during the very last step of bootstrap.
 */
public interface MappingFinalizationContext {

	ConfigurationPropertySource getConfigurationPropertySource();

}
