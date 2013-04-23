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

import javax.transaction.Synchronization;

/**
 * Contract needed by Hibernate Search to batch changes per transaction.
 *
 * @author Navin Surtani  - navin@surtani.org
 */
public interface TransactionContext {
	/**
	 * @return A boolean indicating whether a transaction is in progress or not.
	 */
	public boolean isTransactionInProgress();

	/**
	 * @return a transaction object.
	 */
	public Object getTransactionIdentifier();

	/**
	 * Register the given synchronization.
	 *
	 * @param synchronization synchronization to register
	 */
	public void registerSynchronization(Synchronization synchronization);
}
