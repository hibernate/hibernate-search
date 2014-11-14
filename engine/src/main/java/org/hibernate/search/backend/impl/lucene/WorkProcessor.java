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
package org.hibernate.search.backend.impl.lucene;

import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;


/**
 * Defines the contract for async and synchronous processors to apply
 * batched of indexing work to the index.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
interface WorkProcessor {

	/**
	 * Prepare for the queue to be shut down.
	 * Needs to flush pending work and shutdown any internal threads.
	 */
	void shutdown();

	/**
	 * Enqueues a new batch of indexing work to be applied.
	 * @param workList the list of work
	 * @param monitor any optional listener which needs to be notified for the work.
	 */
	void submit(List<LuceneWork> workList, IndexingMonitor monitor);

	/**
	 * Only invoked when some dynamic parameters are reconfigured
	 * @param resources the new instance to be used
	 */
	void updateResources(LuceneBackendResources resources);

}
