/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.support;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.weld.transaction.spi.TransactionServices;

import com.arjuna.ats.jta.common.jtaPropertyManager;

public class TestingTransactionServices implements TransactionServices {

	@Override
	public void cleanup() {
	}

	@Override
	public void registerSynchronization(Synchronization synchronizedObserver) {
		jtaPropertyManager.getJTAEnvironmentBean()
				.getTransactionSynchronizationRegistry()
				.registerInterposedSynchronization( synchronizedObserver );
	}

	@Override
	public boolean isTransactionActive() {
		try {
			return com.arjuna.ats.jta.UserTransaction.userTransaction().getStatus() == Status.STATUS_ACTIVE;
		}
		catch (SystemException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public UserTransaction getUserTransaction() {
		return com.arjuna.ats.jta.UserTransaction.userTransaction();
	}
}
