package org.hibernate.search.batchindexing.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard
 */
class Helper {

	/**
	 * if the transaction object is a JoinableCMTTransaction, call markForJoined()
	 * This must be done prior to starting the transaction
	 */
	public static Transaction getTransactionAndMarkForJoin(StatelessSession session)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
		Transaction transaction = session.getTransaction();
		doMarkforJoined( transaction );
		return transaction;
	}

	/**
	 * if the transaction object is a JoinableCMTTransaction, call markForJoined()
	 * This must be done prior to starting the transaction
	 */
	public static Transaction getTransactionAndMarkForJoin(Session session)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Transaction transaction = session.getTransaction();
		doMarkforJoined( transaction );
		return transaction;
	}

	private static void doMarkforJoined(Transaction transaction)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if ( transaction.getClass().getName().equals( "org.hibernate.ejb.transaction.JoinableCMTTransaction" ) ) {
			Class<?> joinableCMTTransaction = ClassLoaderHelper.classForName(
					"org.hibernate.ejb.transaction.JoinableCMTTransaction",
					Helper.class.getClassLoader()
			);
			final Method markForJoined = joinableCMTTransaction.getMethod( "markForJoined" );
			markForJoined.invoke( transaction );
		}
	}

}
