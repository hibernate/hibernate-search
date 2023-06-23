/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.facet;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test behavior when performing faceting requests on an index with no value for the targeted faceted field
 * (either because there is no document, or because no document has a value for this field).
 */
@TestForIssue(jiraKey = "HSEARCH-2955")
@Category(PortedToSearch6.class)
public class NoIndexedValueFacetingTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( FacetedEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void discrete_emptyIndex() {
		doDiscreteFacetingQuery();
	}

	@Test
	public void integerRange_emptyIndex() {
		doIntegerRangeFacetingQuery();
	}

	@Test
	public void floatRange_emptyIndex() {
		doFloatRangeFacetingQuery();
	}

	@Test
	public void discrete_noIndexedValueForFacet() {
		helper.index(
				new FacetedEntity( 0, null, 0, 0.01f ),
				new FacetedEntity( 1, null, 1, 42.01f )
		);
		doDiscreteFacetingQuery();
	}

	@Test
	public void integerRange_noIndexedValueForFacet() {
		helper.index(
				new FacetedEntity( 0, "first", null, 0.01f ),
				new FacetedEntity( 1, "second", null, 42.01f )
		);
		doIntegerRangeFacetingQuery();
	}

	@Test
	public void floatRange_noIndexedValueForFacet() {
		helper.index(
				new FacetedEntity( 0, "first", 0, null ),
				new FacetedEntity( 1, "second", 1, null )
		);
		doFloatRangeFacetingQuery();
	}

	private void doDiscreteFacetingQuery() {
		QueryBuilder qb = helper.queryBuilder( FacetedEntity.class );
		FacetingRequest request = qb.facet()
				.name( "myRequest" )
				.onField( "stringField" )
				.discrete()
				.includeZeroCounts( false )
				.createFacetingRequest();

		// Mainly, we're testing that executing the query with faceting enabled won't explode
		HSQuery hsQuery = helper.hsQuery( FacetedEntity.class );
		hsQuery.getFacetManager().enableFaceting( request );
		helper.assertThatQuery( hsQuery )
				.facets( "myRequest" )
				.isEmpty();
	}

	private void doIntegerRangeFacetingQuery() {
		QueryBuilder qb = helper.queryBuilder( FacetedEntity.class );
		FacetingRequest request = qb.facet()
				.name( "myRequest" )
				.onField( "integerField" )
				.range()
				.from( 0 ).to( 1000 )
				.from( 1001 ).to( 1500 )
				.includeZeroCounts( true )
				.createFacetingRequest();

		// Mainly, we're testing that executing the query with faceting enabled won't explode
		HSQuery hsQuery = helper.hsQuery( FacetedEntity.class );
		hsQuery.getFacetManager().enableFaceting( request );
		helper.assertThatQuery( hsQuery )
				.facets( "myRequest" )
				.includes( "[0, 1000]", 0 )
				.includes( "[1001, 1500]", 0 )
				.only();
	}

	private void doFloatRangeFacetingQuery() {
		QueryBuilder qb = helper.queryBuilder( FacetedEntity.class );
		FacetingRequest request = qb.facet()
				.name( "myRequest" )
				.onField( "floatField" )
				.range()
				.from( 0.0f ).to( 1000.0f ).excludeLimit()
				.from( 1000.0f ).to( 1500.0f ).excludeLimit()
				.includeZeroCounts( true )
				.createFacetingRequest();

		// Mainly, we're testing that executing the query with faceting enabled won't explode
		HSQuery hsQuery = helper.hsQuery( FacetedEntity.class );
		hsQuery.getFacetManager().enableFaceting( request );
		helper.assertThatQuery( hsQuery )
				.facets( "myRequest" )
				.includes( "[0.0, 1000.0)", 0 )
				.includes( "[1000.0, 1500.0)", 0 )
				.only();
	}

	@Indexed
	private static class FacetedEntity {
		@DocumentId
		private final int id;

		@Field(analyze = Analyze.NO)
		@Facet
		private final String stringField;

		@Field(analyze = Analyze.NO)
		@Facet
		private final Integer integerField;

		@Field(analyze = Analyze.NO)
		@Facet
		private final Float floatField;

		public FacetedEntity(int id, String stringField, Integer integerField, Float floatField) {
			this.id = id;
			this.stringField = stringField;
			this.integerField = integerField;
			this.floatField = floatField;
		}
	}
}
