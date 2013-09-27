/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
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
final class Helper {

	private Helper() {
		//not allowed
	}

	/**
	 * if the transaction object is a JoinableCMTTransaction, call markForJoined()
	 * This must be done prior to starting the transaction
	 */
	public static Transaction getTransactionAndMarkForJoin(StatelessSession session)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
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
		if ( "org.hibernate.ejb.transaction.JoinableCMTTransaction".equals( transaction.getClass().getName() ) ) {
			Class<?> joinableCMTTransaction = ClassLoaderHelper.classForName(
					"org.hibernate.ejb.transaction.JoinableCMTTransaction",
					Helper.class.getClassLoader()
			);
			final Method markForJoined = joinableCMTTransaction.getMethod( "markForJoined" );
			markForJoined.invoke( transaction );
		}
	}

}
