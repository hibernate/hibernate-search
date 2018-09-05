/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import org.apache.lucene.search.NumericRangeQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test to verify that we allow to encode the DocumentId field as a NumericField
 *
 * @author Sanne Grinovero
 */
public class NumericIdEncodingTest {

	@Rule
	public final SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Staff.class );

	private final SearchITHelper helper = new SearchITHelper( factoryHolder );

	@Test
	public void testNumericIdRangeQuery() {
		helper.index(
				new Staff( 1l, "One" ),
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
		SearchIntegrator searchFactory = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = helper.queryBuilder( Staff.class );
		HSQuery hsQuery = searchFactory.createHSQuery( numericRangeQuery, Staff.class )
				.projection( "name" )
				.sort( queryBuilder.sort().byField( "idSort" ).createSort() );
		helper.assertThat( hsQuery )
				.matchesExactlySingleProjections( expectedProjections )
				.hasResultSize( expectedProjections.length );
	}

	@Indexed
	public class Staff {

		@DocumentId
		@NumericField(forField = "id")
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		final Long id;

		@Field(store = Store.YES)
		final String name;

		Staff(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
