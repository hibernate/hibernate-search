package org.hibernate.search.test.integration.jbossjta.infra;

import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * @author Emmanuel Bernard
 */
public class JBossTSStandaloneTransactionManagerLookup implements TransactionManagerLookup {
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		//TODO use reflection
		return new TransactionManagerImple();
	}

	public String getUserTransactionName() {
		return null;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
