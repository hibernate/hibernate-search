/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * A start context for mappings.
 */
public interface MappingStartContext {

	/**
	 * A collector of (non-fatal) failures, allowing notification of Hibernate Search
	 * that something went wrong and an exception should be thrown at some point,
	 * while still continuing the bootstrap process for some time to collect other errors
	 * that could be relevant to users.
	 *
	 * @return A failure collector.
	 */
	ContextualFailureCollector failureCollector();

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	ConfigurationPropertySource configurationPropertySource();

}
