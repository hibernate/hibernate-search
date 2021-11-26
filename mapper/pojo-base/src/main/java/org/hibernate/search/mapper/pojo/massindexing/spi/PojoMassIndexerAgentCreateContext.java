/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.concurrent.ScheduledExecutorService;

public interface PojoMassIndexerAgentCreateContext {

	/**
	 * Creates a new fixed size {@link ScheduledExecutorService}.
	 * <p>
	 * The queue size is not capped, so users should take care of checking they submit a reasonable amount of tasks.
	 *
	 * @param threads the number of threads
	 * @param threadNamePrefix a label to identify the threads; useful for profiling.
	 * @return the new ExecutorService
	 */
	ScheduledExecutorService newScheduledExecutor(int threads, String threadNamePrefix);

	String tenantIdentifier();

}
