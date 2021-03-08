/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.Transaction;

public final class TransactionUtils {

	private TransactionUtils() {
	}

	public static <R> R withinTransaction(Session session, Supplier<R> action) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			R result = action.get();
			tx.commit();
			return result;
		}
		catch (Throwable t) {
			if ( tx == null ) {
				throw t;
			}
			try {
				tx.rollback();
			}
			catch (AssertionError e) {
				// An assertion failed while rolling back...
				// Propagate the assertion failure, but make sure to add some context
				e.addSuppressed( t );
				throw e;
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}
}
