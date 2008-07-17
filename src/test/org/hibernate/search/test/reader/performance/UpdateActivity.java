// $Id$
package org.hibernate.search.test.reader.performance;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.reader.Detective;

/**
 * @author Sanne Grinovero
 */
public class UpdateActivity extends AbstractActivity {

	UpdateActivity(SessionFactory sf, CountDownLatch startSignal) {
		super(sf, startSignal);
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		FullTextQuery q = getQuery( "John", s, Detective.class );
		List list = q.setMaxResults( 1 ).list();
		for ( Object o : list){
			Detective detective = (Detective) o;
			detective.setPhysicalDescription( "old" );
		}
	}

}
