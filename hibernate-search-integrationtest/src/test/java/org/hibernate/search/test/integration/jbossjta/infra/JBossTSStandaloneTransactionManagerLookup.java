package org.hibernate.search.test.integration.jbossjta.infra;

import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Return a standalone JTA transaction manager for JBoss Transactions
 * 
 * @author Emmanuel Bernard
 */
public class JBossTSStandaloneTransactionManagerLookup implements TransactionManagerLookup {

	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			@SuppressWarnings( "unchecked" )
			Class<? extends TransactionManager> clazz = (Class<? extends TransactionManager>)
					Class.forName("com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple");
			return clazz.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	public String getUserTransactionName() {
		return null;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
