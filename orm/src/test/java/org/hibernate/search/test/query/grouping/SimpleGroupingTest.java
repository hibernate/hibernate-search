/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.grouping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.grouping.GroupingRequest;
import org.hibernate.search.query.grouping.GroupingResult;
import org.junit.Test;

/**
 * @author Sascha Grebe
 */
public class SimpleGroupingTest extends AbstractGroupingTest {
	
	private final String indexFieldName = "color";
	
	@Test
	public void testSimpleGrouping() throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group()
				.onField( indexFieldName )
				.topGroupCount(10)
				.createGroupingRequest();
		
		final FullTextQuery query = queryHondaWithGrouping( request );
		
		final GroupingResult groups = query.getGroupingManager().getGroupingResult();
		assertEquals( "Wrong number of total groups", 5, groups.getTotalGroupCount().intValue() );
	}

	@Test
	public void testWithMaxGroupLimit() throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group()
				.onField( indexFieldName )
				.topGroupCount(1)
				.createGroupingRequest();
		
		final FullTextQuery query = queryHondaWithGrouping( request );
		
		final GroupingResult groups = query.getGroupingManager().getGroupingResult();
		assertEquals( "Wrong number of total groups", 5, groups.getTotalGroupCount().intValue() );
		assertEquals( "Wrong number of returned groups", 1, groups.getGroups().size() );
	}
	
	@Test
	public void testWithMaxDocPerGroupLimit() throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group()
				.onField( indexFieldName )
				.topGroupCount(2)
				.maxDocsPerGroup(1)
				.createGroupingRequest();
		
		final FullTextQuery query = queryHondaWithGrouping( request );
		
		final GroupingResult groups = query.getGroupingManager().getGroupingResult();
		assertEquals( "Wrong number of total groups", 5, groups.getTotalGroupCount().intValue() );
		// returns the sum of docs in group no matter how many loaded
		assertEquals( "Wrong number of total hits in groups", 6, groups.getTotalGroupedHitCount() );
	}
	
	@Test
	public void testWithGroupOffset() throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group()
				.onField( indexFieldName )
				.topGroupCount(2)
				.groupOffset(3)
				.createGroupingRequest();
		
		final FullTextQuery query = queryHondaWithGrouping( request );
		
		final GroupingResult groups = query.getGroupingManager().getGroupingResult();
		// returns the sum of docs in group no matter how many loaded
		assertEquals( "Wrong number of total hits in groups", 4, groups.getTotalGroupedHitCount() );
	}
	
	@Test
	public void testOmmitTotalHitCount() throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group()
				.onField( indexFieldName )
				.topGroupCount(10)
				.calculateTotalGroupCount(false)
				.createGroupingRequest();
		
		final FullTextQuery query = queryHondaWithGrouping( request );
		
		final GroupingResult groups = query.getGroupingManager().getGroupingResult();
		assertNull( "Wrong number of total hits in groups", groups.getTotalGroupCount() );
	}
	
	private FullTextQuery queryHondaWithGrouping(GroupingRequest request) {
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getGroupingManager().group( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
		return query;
	}

	@Override
	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( String make : makes ) {
			for ( String color : colors ) {
				for ( int cc : ccs ) {
					Car car = new Car( make, color, cc );
					session.save( car );
				}
			}
		}
		
		Car car = new Car( "Honda", "yellow", 2407 );
		session.save( car );

		car = new Car( "Ford", "yellow", 2500 );
		session.save( car );
		tx.commit();
		session.clear();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class
		};
	}
}
