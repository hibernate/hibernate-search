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
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.spi.Work;

/**
 * Pile work operations
 * No thread safety has to be implemented, the queue being scoped already
 * The implementation must be "stateless" wrt the queue through (ie not store the queue state)
 *
 * FIXME this Interface does not make much sense, since the impl will not be changed
 *
 * @author Emmanuel Bernard
 */
public interface QueueingProcessor {
	/**
	 * Add a work
	 * TODO move that somewhere else, it does not really fit here
	 */
	void add(Work work, WorkQueue workQueue);

	/**
	 * prepare resources for a later performWorks call
	 */
	void prepareWorks(WorkQueue workQueue);

	/**
	 * Execute works
	 */
	void performWorks(WorkQueue workQueue);

	/**
	 * Rollback works
	 */
	void cancelWorks(WorkQueue workQueue);

}
