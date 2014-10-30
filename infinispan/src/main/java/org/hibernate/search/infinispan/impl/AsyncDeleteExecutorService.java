/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.impl;

import java.util.concurrent.Executor;

import org.hibernate.search.engine.service.spi.Service;

/**
 * Defines the service contract for the Executor which we'll use in
 * combination with the Infinispan Lucene Directory, as this provides
 * an option to execute delete operations in background.
 * It is important to run delete operations in background as while these
 * are simple from a computational point of view, they will introduce a
 * significant delay on write operations when Infinispan is running in
 * clustered mode.
 * This is implemented as a Service so that integrations can inject a
 * different managed threadpool, and we can share the same executor
 * among multiple IndexManagers.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
public interface AsyncDeleteExecutorService extends Service {

	Executor getExecutor();

	void closeAndFlush();

}
