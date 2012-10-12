/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.backend.impl.batch;


import java.util.Set;

import org.hibernate.search.backend.LuceneWork;

/**
 * Implementations of this interface are not drop-in replacements for the standard BackendQueueProcessor,
 * but are meant to be used only during batch processing.
 * The order of LuceneWork(s) processed is not guaranteed as the queue is consumed by several
 * concurrent workers.
 *
 * @author Sanne Grinovero
 */
public interface BatchBackend {

	/**
	 * Enqueues one work to be processed asynchronously
	 *
	 * @param work a {@link org.hibernate.search.backend.LuceneWork} object.
	 * @throws java.lang.InterruptedException if the current thread is interrupted while
	 *                              waiting for the work queue to have enough space.
	 */
	void enqueueAsyncWork(LuceneWork work) throws InterruptedException;

	/**
	 * Does one work in sync
	 *
	 * @param work
	 *
	 * @throws InterruptedException
	 */
	void doWorkInSync(LuceneWork work);

	/**
	 * Since most work is done async in the backend, we need to flush at the end to
	 * make sure we don't return control before all work was processed,
	 * and that IndexWriters are committed or closed.
	 *
	 * @param indexedRootTypes a {@link java.util.Set} object.
	 */
	void flush(Set<Class<?>> indexedRootTypes);

	/**
	 * Triggers optimization of all indexes containing at least one instance of the
	 * listed targetedClasses.
	 *
	 * @param targetedClasses Used to specify which indexes need optimization.
	 */
	void optimize(Set<Class<?>> targetedClasses);

}
