/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jbossjta.infra;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

public class NarayanaStandaloneJtaPlatform extends AbstractJtaPlatform {

	public NarayanaStandaloneJtaPlatform() {
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			return com.arjuna.ats.jta.TransactionManager.transactionManager();
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		try {
			return com.arjuna.ats.jta.UserTransaction.userTransaction();
		}
		catch (Exception e) {
			throw new JtaPlatformException( "Could not obtain JBoss Transactions user transaction instance", e );
		}
	}

}
