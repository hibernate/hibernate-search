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

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

/**
 * Return a standalone JTA transaction manager for JBoss Transactions
 *
 * @author Emmanuel Bernard
 */
public class JBossTSStandaloneTransactionManagerLookup implements JtaPlatform {

	public TransactionManager retrieveTransactionManager() {
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
		catch (Exception e) {
			throw new HibernateException( "Could not obtain JBoss Transactions transaction manager instance", e );
		}
	}

	public UserTransaction retrieveUserTransaction() {
		return null;
	}

	public boolean canRegisterSynchronization() {
		return JtaStatusHelper.isActive( retrieveTransactionManager() );
	}

	public void registerSynchronization(Synchronization synchronization) {
		try {
			retrieveTransactionManager().getTransaction().registerSynchronization( synchronization );
		}
		catch (Exception e) {
			throw new TransactionException( "Could not obtain transaction from TM" );
		}
	}

	public int getCurrentStatus() throws SystemException {
		return JtaStatusHelper.getStatus( retrieveTransactionManager() );
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
