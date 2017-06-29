/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * @author Tomas Hradec
 */
public class BatchSupport {

	private static final long batchSize = 1000;

	private final SessionFactory sf;
	private final long initialOffset;

	public BatchSupport(SessionFactory sf, long initialOffset) {
		this.sf = sf;
		this.initialOffset = initialOffset;
	}

	public void execute(final String sql, long totalCount, final BatchCallback batchCallback) {
		Session s = sf.openSession();
		try {
			long iterationCount = totalCount / batchSize;
			if ( iterationCount == 0 ) {
				iterationCount = 1;
			}
			for ( long iterationIndex = 0; iterationIndex < iterationCount; iterationIndex++ ) {
				final long idOffset = initialOffset + ( iterationIndex * batchSize );
				final long idCount = initialOffset + ( iterationIndex * batchSize ) + batchSize - 1;

				final SessionImplementor session = (SessionImplementor) s;
				session.getTransactionCoordinator().createIsolationDelegate().delegateWork(
					new WorkExecutorVisitable() {
					@Override
					public Object accept(WorkExecutor executor, Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( sql );
						for ( long id = idOffset; id < idCount; id++ ) {
							batchCallback.initStatement( ps, id );
							ps.addBatch();
						}
						ps.executeBatch();
						ps.close();
						return null;
					}
				}, true );
			}
		}
		finally {
			s.close();
		}
	}

}
