/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.grouping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.grouping.Group;
import org.hibernate.search.query.grouping.GroupingRequest;
import org.hibernate.search.query.grouping.GroupingResult;
import org.junit.Test;

/**
 * @author Sascha Grebe
 */
public class GroupingSortTest extends AbstractGroupingTest {

	private final String indexFieldName = "color";

	@Test
	public void testSortGroupsByColor() throws Exception {
		testSortGroupsByColor( false );
	}

	@Test
	public void testSortGroupsByColorInverted() throws Exception {
		testSortGroupsByColor( true );
	}

	private void testSortGroupsByColor(boolean invert) throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group().onField( indexFieldName ).topGroupCount( 10 )
				.groupSort( new Sort( new SortField( indexFieldName, Type.STRING, invert ) ) ).createGroupingRequest();

		final FullTextQuery query = queryHondaWithGrouping( request );

		final GroupingResult groups = query.getGroupingManager().getGroupingResult();

		// check groups sorted by color
		Group lastGroup = null;
		for ( Group nextGroup : groups.getGroups() ) {
			if ( lastGroup != null && !invert ) {
				assertTrue( lastGroup.getValue().compareTo( nextGroup.getValue() ) < 0 );

			}
			else if ( lastGroup != null && invert ) {
				assertTrue( lastGroup.getValue().compareTo( nextGroup.getValue() ) > 0 );

			}
			lastGroup = nextGroup;
		}
	}

	@Test
	public void testSortEntitiesInGroupByCubicCapacity() throws Exception {
		testSortEntitiesInGroupByCubicCapacity( false );
	}

	@Test
	public void testSortEntitiesInGroupByCubicCapacityInvert() throws Exception {
		testSortEntitiesInGroupByCubicCapacity( true );
	}

	public void testSortEntitiesInGroupByCubicCapacity(boolean invert) throws Exception {
		final GroupingRequest request = queryBuilder( Car.class ).group().onField( indexFieldName ).topGroupCount( 10 ).maxDocsPerGroup( 10 )
				.withinGroupSort( new Sort( new SortField( "cubicCapacity", Type.INT, invert ) ) ).createGroupingRequest();

		final FullTextQuery query = queryHondaWithGrouping( request );

		final GroupingResult groups = query.getGroupingManager().getGroupingResult();

		// check entities in groups sorted by cubic capacity
		for ( Group nextGroup : groups.getGroups() ) {
			final Session session = this.getSession();
			Car lastCar = null;
			for ( EntityInfo nextEntityInfo : nextGroup.getHits() ) {
				final Car car = (Car) session.load( nextEntityInfo.getClazz(), nextEntityInfo.getId() );
				if ( lastCar != null && !invert ) {
					assertTrue( lastCar.getCubicCapacity() < car.getCubicCapacity() );
				}
				else if ( lastCar != null && invert ) {
					assertTrue( lastCar.getCubicCapacity() > car.getCubicCapacity() );
				}
				lastCar = car;
			}
		}
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
		return new Class[] { Car.class };
	}
}
