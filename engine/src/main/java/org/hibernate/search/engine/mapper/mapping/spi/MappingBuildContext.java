/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.environment.service.spi.ServiceManager;
import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;
import org.hibernate.search.util.SearchException;

/**
 * A build context for mappings.
 */
public interface MappingBuildContext {

	ServiceManager getServiceManager();

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
	ContextualFailureCollector getFailureCollector();

}
