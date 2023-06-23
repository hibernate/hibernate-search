/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.SearchException;

/**
 * A build context for mappings.
 */
public interface MappingBuildContext {

	ClassResolver classResolver();

	ResourceResolver resourceResolver();

	BeanResolver beanResolver();

	/**
	 * A collector of (non-fatal) failures, allowing to notify Hibernate Search
	 * that something went wrong and bootstrap should be aborted at some point,
	 * while still continuing the bootstrap process for some time to collect other errors
	 * that could be relevant to users.
	 * <p>
	 * Note that this is primarily intended for mapper implementations.
	 * Backend implementations should use the collector with caution:
	 * in many cases it's a better idea to throw a {@link SearchException}
	 * so that the mapper can catch it and add it to the failure collector,
	 * optionally prepending some context of its own.
	 *
	 * @return A failure collector.
	 */
	ContextualFailureCollector failureCollector();

	FailureHandler failureHandler();

	ThreadPoolProvider threadPoolProvider();

	ConfigurationPropertySource configurationPropertySource();

}
