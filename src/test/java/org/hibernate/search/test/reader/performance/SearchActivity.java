// $Id$
package org.hibernate.search.test.reader.performance;

import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.reader.Detective;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class SearchActivity extends AbstractActivity {
	
	SearchActivity(SessionFactory sf, CountDownLatch startSignal) {
		super(sf, startSignal);
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		FullTextQuery q = getQuery( "John Doe", s, Detective.class);
		q.setMaxResults( 10 );
		q.getResultSize();
	}
	
}
