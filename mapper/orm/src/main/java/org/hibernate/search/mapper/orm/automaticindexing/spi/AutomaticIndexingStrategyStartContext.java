/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public interface AutomaticIndexingStrategyStartContext {

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

	/**
	 * @return A configuration property source, appropriately masked so that the strategy
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "synchronization" and any sub-keys are reserved.
	 */
	ConfigurationPropertySource configurationPropertySource();

	/**
	 * @return A provided of thread pools, to use when spawning background processes.
	 */
	ThreadPoolProvider threadPoolProvider();

	/**
	 * @return The mapping, providing all information and operations necessary
	 * for background processing of automatic indexing events.
	 */
	AutomaticIndexingMappingContext mapping();

}
