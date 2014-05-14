/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;

/**
 * @author Sanne Grinovero
 */
public class UpdateActivity extends AbstractActivity {

	UpdateActivity(SessionFactory sf, CountDownLatch startSignal) {
		super( sf, startSignal );
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		FullTextQuery q = getQuery( "John", s, Detective.class );
		List list = q.setMaxResults( 1 ).list();
		for ( Object o : list ) {
			Detective detective = (Detective) o;
			detective.setPhysicalDescription( "old" );
		}
	}
}
