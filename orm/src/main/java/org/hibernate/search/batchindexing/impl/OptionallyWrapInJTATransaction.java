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

import javax.transaction.SystemException;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Wrap the subsequent Runnable in a JTA Transaction if necessary:
 *  - if the existing Hibernate Core transaction strategy requires a TransactionManager
 *  - if no JTA transaction is already started
 *
 * Unfortunately at this time we need to have access to SessionFactoryImplementor
 *
 * @author Emmanuel Bernard
 */
public class OptionallyWrapInJTATransaction implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final SessionAwareRunnable sessionAwareRunnable;
	private final StatelessSessionAwareRunnable statelessSessionAwareRunnable;
	private final BatchTransactionalContext batchContext;

	public OptionallyWrapInJTATransaction(BatchTransactionalContext batchContext, SessionAwareRunnable sessionAwareRunnable) {
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		this.batchContext = batchContext;
		this.sessionAwareRunnable = sessionAwareRunnable;
		this.statelessSessionAwareRunnable = null;
	}

	public OptionallyWrapInJTATransaction(BatchTransactionalContext batchContext, StatelessSessionAwareRunnable statelessSessionAwareRunnable) {
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		this.batchContext = batchContext;
		this.sessionAwareRunnable = null;
		this.statelessSessionAwareRunnable = statelessSessionAwareRunnable;
	}

	@Override
	public void run() {
		try {
			final boolean wrapInTransaction = batchContext.wrapInTransaction();
			if ( wrapInTransaction ) {
				try {
					final Session session;
					final StatelessSession statelessSession;
					if ( sessionAwareRunnable != null ) {
						session = batchContext.factory.openSession();
						statelessSession = null;
					}
					else {
						session = null;
						statelessSession = batchContext.factory.openStatelessSession();
					}

					batchContext.transactionManager.begin();

					if ( sessionAwareRunnable != null ) {
						sessionAwareRunnable.run( session );
					}
					else {
						statelessSessionAwareRunnable.run( statelessSession );
					}

					batchContext.transactionManager.commit();

					if ( sessionAwareRunnable != null ) {
						session.close();
					}
					else {
						statelessSession.close();
					}
				}
				catch (Throwable e) {
					batchContext.errorHandler.handleException( log.massIndexerUnexpectedErrorMessage() , e);
					try {
						batchContext.transactionManager.rollback();
					}
					catch (SystemException e1) {
						// we already have an exception, don't propagate this one
						log.errorRollingBackTransaction( e.getMessage(), e1 );
					}
				}
			}
			else {
				if ( sessionAwareRunnable != null ) {
					sessionAwareRunnable.run( null );
				}
				else {
					statelessSessionAwareRunnable.run( null );
				}
			}
		}
		catch (Throwable e) {
			batchContext.errorHandler.handleException( log.massIndexerUnexpectedErrorMessage() , e);
		}
	}
}
