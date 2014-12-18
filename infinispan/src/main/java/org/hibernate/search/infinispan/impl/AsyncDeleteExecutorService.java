/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.infinispan.impl;

import java.util.concurrent.Executor;

import org.hibernate.search.spi.ServiceProvider;

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
public interface AsyncDeleteExecutorService extends ServiceProvider<AsyncDeleteExecutorService> {

	void closeAndFlush();

	Executor getExecutor();

}
