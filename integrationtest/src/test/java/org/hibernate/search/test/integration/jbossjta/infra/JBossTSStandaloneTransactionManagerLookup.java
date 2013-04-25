/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
			return (TransactionManager) getTransactionManager.invoke( jtaEnvironmentBean );
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
