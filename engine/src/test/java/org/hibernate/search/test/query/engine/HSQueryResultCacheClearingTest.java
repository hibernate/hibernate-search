/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.engine;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that the HSQuery result cache gets cleared as necessary.
 *
 * @author Yoann Rodiere
 */
public class HSQueryResultCacheClearingTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void setUp() throws Exception {
		helper.add( new IndexedEntity( 0, "zero" ) );
		helper.add( new IndexedEntity( 1, "one" ) );
		helper.add( new IndexedEntity( 2, "two" ) );
	}

	@Test
	public void clear_firstResult() throws Exception {
		HSQuery hsQuery = queryAll();
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.firstResult( 1 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 1, 2 );

		hsQuery.firstResult( 2 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 2 );
	}

	@Test
	public void clear_maxResult() throws Exception {
		HSQuery hsQuery = queryAll();
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.maxResults( 2 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1 );

		hsQuery.maxResults( 1 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 0 );
	}

	@Test
	public void clear_projection() throws Exception {
		HSQuery hsQuery = queryAll();
		hsQuery.projection( ProjectionConstants.ID );
		helper.assertThat( hsQuery ).matchesExactlySingleProjections( 0, 1, 2 );

		hsQuery.projection( ProjectionConstants.ID, "field" );
		helper.assertThat( hsQuery ).matchesExactlyProjections(
				new Object[][]{
						{ 0, "zero" },
						{ 1, "one" },
						{ 2, "two" },
				} );
	}

	@Test
	public void clear_sort() throws Exception {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		HSQuery hsQuery = queryAll();
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.sort( qb.sort().byField( "idSort" ).desc().createSort() );
		helper.assertThat( hsQuery ).matchesExactlyIds( 2, 1, 0 );
	}

	@Test
	public void clear_faceting() throws Exception {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		HSQuery hsQuery = queryAll();
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		String facetingRequestName = "facet1";
		FacetingRequest facetingRequest1 = qb.facet()
				.name( facetingRequestName )
				.onField( "facetField" )
				.discrete()
				.createFacetingRequest();
		hsQuery.getFacetManager().enableFaceting( facetingRequest1 );
		helper.assertThat( hsQuery ).facets( facetingRequestName )
				.includes( "zero", 1 )
				.includes( "one", 1 )
				.includes( "two", 1 )
				.only();

		hsQuery.getFacetManager().disableFaceting( facetingRequestName );
		helper.assertThat( hsQuery ).facets( facetingRequestName ).isEmpty();
	}

	@Test
	public void clear_fullTextFilter() throws Exception {
		HSQuery hsQuery = queryAll();
		helper.assertThat( hsQuery ).hasResultSize( 3 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		String filterName = "keepOnlyValueOne";
		hsQuery.enableFullTextFilter( filterName );
		helper.assertThat( hsQuery ).hasResultSize( 1 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 1 );

		hsQuery.disableFullTextFilter( filterName );
		helper.assertThat( hsQuery ).hasResultSize( 3 );
		helper.assertThat( hsQuery ).matchesExactlyIds( 0, 1, 2 );
	}

	private HSQuery queryAll() {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		return helper.hsQuery( IndexedEntity.class )
				.sort( qb.sort().byField( "idSort" ).createSort() );
	}

	@Indexed
	@FullTextFilterDef(name = "keepOnlyValueOne", impl = KeepOnlyValueOneFilter.class)
	private static class IndexedEntity {
		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		private Integer id;

		@Field(store = Store.YES, analyze = Analyze.NO)
		@SortableField
		@Field(name = "facetField", analyze = Analyze.NO)
		@Facet(forField = "facetField")
		private String field;

		public IndexedEntity(Integer id, String field) {
			super();
			this.id = id;
			this.field = field;
		}
	}

	public static class KeepOnlyValueOneFilter {
		@Factory
		public Query create() {
			return new BooleanQuery.Builder()
					.add( new TermQuery( new Term( "field", "one" ) ), Occur.MUST )
					.build();
		}
	}

}
