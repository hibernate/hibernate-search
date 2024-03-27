/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
