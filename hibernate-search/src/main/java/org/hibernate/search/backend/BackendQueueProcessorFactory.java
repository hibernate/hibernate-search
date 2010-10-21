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
package org.hibernate.search.backend;

import java.util.Properties;
import java.util.List;

import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Interface for different types of queue processor factories. Implementations need a no-arg constructor.
 * The factory typically prepares or pools the resources needed by the queue processor.
 *
 * @author Emmanuel Bernard
 */
public interface BackendQueueProcessorFactory {
	
	/**
	 * Used at startup, called once as first method.
	 * @param props all configuration properties
	 * @param context context giving access to required meta data
	 */
	void initialize(Properties props, WorkerBuildContext context);

	/**
	 * Return a runnable implementation responsible for processing the queue to a given backend.
	 *
	 * @param queue The work queue to process.
	 * @return <code>Runnable</code> which processes <code>queue</code> when started.
	 */
	Runnable getProcessor(List<LuceneWork> queue);
	
	/**
	 * Used to shutdown and eventually release resources.
	 * No other method should be used after this one.
	 */
	void close();
	
}
