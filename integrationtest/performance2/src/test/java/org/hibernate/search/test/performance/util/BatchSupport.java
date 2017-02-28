/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.performance.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;

/**
 * @author Tomas Hradec
 */
public class BatchSupport {

	private static final long batchSize = 1000;
	private static final long initialOffset = 1000 * 1000;

	private final SessionFactory sf;

	public BatchSupport(SessionFactory sf) {
		this.sf = sf;
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

				Transaction tx = s.beginTransaction();
				s.doWork( new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( sql );
						for ( long id = idOffset; id < idCount; id++ ) {
							batchCallback.initStatement( ps, id );
							ps.addBatch();
						}
						ps.executeBatch();
						ps.close();
					}
				} );
				tx.commit();
			}
		}
		finally {
			s.close();
		}
	}

}
