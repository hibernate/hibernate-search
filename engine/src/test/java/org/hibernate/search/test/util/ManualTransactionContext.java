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
package org.hibernate.search.test.util;

import java.util.List;
import java.util.ArrayList;
import javax.transaction.Synchronization;
import javax.transaction.Status;

import org.hibernate.search.backend.TransactionContext;

/**
 * @author Emmanuel Bernard
 */
public class ManualTransactionContext implements TransactionContext {
	private boolean progress = true;
	private List<Synchronization> syncs = new ArrayList<Synchronization>();

	@Override
	public boolean isTransactionInProgress() {
		return progress;
	}

	@Override
	public Object getTransactionIdentifier() {
		return this;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		syncs.add( synchronization );
	}

	public void end() {
		this.progress = false;
		for ( Synchronization sync : syncs ) {
			sync.beforeCompletion();
		}

		for ( Synchronization sync : syncs ) {
			sync.afterCompletion( Status.STATUS_COMMITTED );
		}
	}
}
