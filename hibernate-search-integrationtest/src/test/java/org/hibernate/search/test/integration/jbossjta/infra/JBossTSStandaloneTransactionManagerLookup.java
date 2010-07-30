package org.hibernate.search.test.integration.jbossjta.infra;

import java.lang.reflect.Method;
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
			//Call jtaPropertyManager.getJTAEnvironmentBean().getTransactionManager();
			
			//improper camel case name for the class
			Class<?> propertyManager = Class.forName( "com.arjuna.ats.jta.common.jtaPropertyManager" );
			final Method getJTAEnvironmentBean = propertyManager.getMethod( "getJTAEnvironmentBean" );
			//static method
			final Object jtaEnvironmentBean = getJTAEnvironmentBean.invoke( null );
			final Method getTransactionManager = jtaEnvironmentBean.getClass().getMethod( "getTransactionManager" );
			return ( TransactionManager ) getTransactionManager.invoke( jtaEnvironmentBean );
		}
		catch ( Exception e ) {
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
