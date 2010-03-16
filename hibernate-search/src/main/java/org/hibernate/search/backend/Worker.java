/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Perform work for a given session. This implementation has to be multi threaded.
 *
 * @author Emmanuel Bernard
 */
public interface Worker {
	//Use of EventSource since it's the common subinterface for Session and SessionImplementor
	//the alternative would have been to do a subcasting or to retrieve 2 parameters :(
	void performWork(Work work, TransactionContext transactionContext);

	void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor);

	/**
	 * clean resources
	 * This method can return exceptions
	 */
	void close();

	/**
	 * Flush any work queue.
	 *
	 * @param transactionContext the current transaction (context).
	 */
	void flushWorks(TransactionContext transactionContext);
}
