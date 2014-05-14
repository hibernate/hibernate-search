/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class SearchActivity extends AbstractActivity {

	SearchActivity(SessionFactory sf, CountDownLatch startSignal) {
		super( sf, startSignal );
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		FullTextQuery q = getQuery( "John Doe", s, Detective.class );
		q.setMaxResults( 10 );
		q.getResultSize();
	}

}
