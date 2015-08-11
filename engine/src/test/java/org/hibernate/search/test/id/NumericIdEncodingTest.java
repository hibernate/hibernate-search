/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.NumericRangeQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test to verify that we allow to encode the DocumentId field as a NumericField
 *
 * @author Sanne Grinovero
 */
public class NumericIdEncodingTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Staff.class );

	@Test
	public void testNumericIdRangeQuery() {
		storeTestingData( new Staff( 1l, "One" ),
				new Staff( 2l, "Two" ),
				new Staff( 3l, "Three" ),
				new Staff( 4l, "Four" )
			);

		NumericRangeQuery<Long> smallRangeQuery = NumericRangeQuery.newLongRange( "id", 1l, 3l, false, false );
		expectedProjections( smallRangeQuery, "Two" );

		NumericRangeQuery<Long> universeRangeQuery = NumericRangeQuery.newLongRange( "id", null, null, false, false );
		expectedProjections( universeRangeQuery, "One", "Two", "Three", "Four" );
	}

	private void expectedProjections(NumericRangeQuery<Long> numericRangeQuery, String... expectedProjections) {
		HSQuery hsQuery = factoryHolder.getSearchFactory().createHSQuery()
				.luceneQuery( numericRangeQuery )
				.targetedEntities( Arrays.<Class<?>>asList( Staff.class ) )
				.projection( "name" );
		List<EntityInfo> result = hsQuery.queryEntityInfos();
		assertEquals( expectedProjections.length, result.size() );
		assertEquals( expectedProjections.length, hsQuery.queryResultSize() );
		for ( int i = 0; i < expectedProjections.length; i++ ) {
			assertEquals( expectedProjections[i], result.get( i ).getProjection()[0] );
		}
	}

	private void storeTestingData(Staff... testData) {
		Worker worker = factoryHolder.getSearchFactory().getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		for ( int i = 0; i < testData.length; i++ ) {
			Staff p = testData[i];
			worker.performWork( new Work( p, p.id, WorkType.INDEX ), tc);
		}
		tc.end();
	}

	@Indexed
	public class Staff {

		@DocumentId @NumericField final Long id;
		@Field(store = Store.YES) final String name;

		Staff(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
