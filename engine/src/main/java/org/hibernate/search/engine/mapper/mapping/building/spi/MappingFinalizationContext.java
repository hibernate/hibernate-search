/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * The context passed to the mapper during the very last step of bootstrap.
 */
public interface MappingFinalizationContext {

	ContextualFailureCollector failureCollector();

	ConfigurationPropertySource configurationPropertySource();

	BeanResolver beanResolver();

}
