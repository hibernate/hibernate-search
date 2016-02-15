/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.factory;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.genericjpa.factory.Transaction;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Martin on 22.06.2015.
 */
public class TransactionTest {

	@Test
	public void testCommit() {
		Transaction tx = new Transaction();
		boolean[] tmp = new boolean[2];
		tx.registerSynchronization(
				new Synchronization() {
					@Override
					public void beforeCompletion() {
						tmp[0] = true;
					}

					@Override
					public void afterCompletion(int status) {
						if ( status == Status.STATUS_COMMITTED ) {
							tmp[1] = true;
						}
					}
				}
		);
		tx.commit();
		for ( boolean t : tmp ) {
			assertTrue( t );
		}
	}

	@Test
	public void testRollback() {
		Transaction tx = new Transaction();
		boolean[] tmp = new boolean[2];
		tx.registerSynchronization(
				new Synchronization() {
					@Override
					public void beforeCompletion() {
						tmp[0] = true;
					}

					@Override
					public void afterCompletion(int status) {
						if ( status == Status.STATUS_ROLLEDBACK ) {
							tmp[1] = true;
						}
					}
				}
		);
		tx.rollback();
		for ( boolean t : tmp ) {
			assertTrue( t );
		}
	}

	@Test
	public void testExceptions() {
		{
			Transaction tx = new Transaction();
			tx.commit();
			try {
				tx.rollback();
				fail( "Exception expected!" );
			}
			catch (Exception e) {

			}
		}

		{
			Transaction tx = new Transaction();
			tx.rollback();
			try {
				tx.commit();
				fail( "Exception expected!" );
			}
			catch (Exception e) {

			}
		}
	}

}
