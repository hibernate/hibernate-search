/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.MatchAllDocsQuery;

@TestForIssue(jiraKey = "HSEARCH-2535")
@Category(PortedToSearch6.class)
public class MultiValuedFacetingTest {
	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder(
			StringArrayFacetEntity.class, StringCollectionFacetEntity.class, StringMapFacetEntity.class,
			NumberArrayFacetEntity.class, NumberCollectionFacetEntity.class, NumberMapFacetEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void stringArray() throws Exception {
		StringArrayFacetEntity entity = new StringArrayFacetEntity( 1L );
		helper.add( entity );
		entity = new StringArrayFacetEntity( 1L, "foo" );
		helper.add( entity );
		entity = new StringArrayFacetEntity( 2L, "foo", "bar" );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), StringArrayFacetEntity.class );

		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( StringArrayFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "foo", 2 );
		assertFacet( facets.get( 1 ), "bar", 1 );
	}

	@Test
	public void stringCollection() throws Exception {
		StringCollectionFacetEntity entity = new StringCollectionFacetEntity( 1L );
		helper.add( entity );
		entity = new StringCollectionFacetEntity( 1L, "foo" );
		helper.add( entity );
		entity = new StringCollectionFacetEntity( 2L, "foo", "bar" );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), StringCollectionFacetEntity.class );

		QueryBuilder builder =
				sfHolder.getSearchFactory().buildQueryBuilder().forEntity( StringCollectionFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "foo", 2 );
		assertFacet( facets.get( 1 ), "bar", 1 );
	}

	@Test
	public void stringMap() throws Exception {
		StringMapFacetEntity entity = new StringMapFacetEntity( 1L );
		helper.add( entity );
		entity = new StringMapFacetEntity( 1L, "foo" );
		helper.add( entity );
		entity = new StringMapFacetEntity( 2L, "foo", "bar" );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), StringMapFacetEntity.class );

		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( StringMapFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "foo", 2 );
		assertFacet( facets.get( 1 ), "bar", 1 );
	}

	@Test
	@Ignore // HSEARCH-1927 : Range faceting on multiple numeric values does not work
	public void numberArray() throws Exception {
		NumberArrayFacetEntity entity = new NumberArrayFacetEntity( 1L );
		helper.add( entity );
		entity = new NumberArrayFacetEntity( 1L, 42 );
		helper.add( entity );
		entity = new NumberArrayFacetEntity( 2L, 43, 442 );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), NumberArrayFacetEntity.class );

		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NumberArrayFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.range()
				.from( 0.0f ).to( 100.0f ).excludeLimit()
				.from( 100.0f ).to( 500.0f ).excludeLimit()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "[0.0, 100.0)", 2 );
		assertFacet( facets.get( 1 ), "[100.0, 500.0)", 1 );
	}

	@Test
	@Ignore // HSEARCH-1927 : Range faceting on multiple numeric values does not work
	public void numberCollection() throws Exception {
		NumberCollectionFacetEntity entity = new NumberCollectionFacetEntity( 1L );
		helper.add( entity );
		entity = new NumberCollectionFacetEntity( 1L, 42.2f );
		helper.add( entity );
		entity = new NumberCollectionFacetEntity( 2L, 42.3f, 442.2f );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), NumberCollectionFacetEntity.class );

		QueryBuilder builder =
				sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NumberCollectionFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.range()
				.from( 0.0f ).to( 100.0f ).excludeLimit()
				.from( 100.0f ).to( 500.0f ).excludeLimit()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "[0.0, 100.0)", 2 );
		assertFacet( facets.get( 1 ), "[100.0, 500.0)", 1 );
	}

	@Test
	@Ignore // HSEARCH-1927 : Range faceting on multiple numeric values does not work
	public void numberMap() throws Exception {
		NumberMapFacetEntity entity = new NumberMapFacetEntity( 1L );
		helper.add( entity );
		entity = new NumberMapFacetEntity( 1L, 42.2f );
		helper.add( entity );
		entity = new NumberMapFacetEntity( 2L, 42.3f, 442.2f );
		helper.add( entity );

		HSQuery hsQuery = helper.hsQuery( new MatchAllDocsQuery(), NumberMapFacetEntity.class );

		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NumberMapFacetEntity.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "facet" )
				.range()
				.from( 0.0f ).to( 100.0f ).excludeLimit()
				.from( 100.0f ).to( 500.0f ).excludeLimit()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = hsQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be two facets", 2, facets.size() );
		assertFacet( facets.get( 0 ), "[0.0, 100.0)", 2 );
		assertFacet( facets.get( 1 ), "[100.0, 500.0)", 1 );
	}

	private void assertFacet(Facet facet, String expectedValue, int expectedCount) {
		assertEquals( "Wrong facet value", expectedValue, facet.getValue() );
		assertEquals( "Wrong facet count", expectedCount, facet.getCount() );
	}

	@Indexed
	private static class StringArrayFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		@org.hibernate.search.annotations.Facet
		private String[] facet;

		private StringArrayFacetEntity(Long id, String... facet) {
			super();
			this.id = id;
			this.facet = facet;
		}
	}

	@Indexed
	private static class StringCollectionFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		@org.hibernate.search.annotations.Facet
		private Collection<String> facet;

		private StringCollectionFacetEntity(Long id, String... facet) {
			super();
			this.id = id;
			this.facet = Arrays.asList( facet );
		}
	}

	@Indexed
	private static class StringMapFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		@org.hibernate.search.annotations.Facet
		private Map<Integer, String> facet;

		private StringMapFacetEntity(Long id, String... facet) {
			super();
			this.id = id;
			this.facet = new TreeMap<>();
			int index = 0;
			for ( String facetValue : facet ) {
				this.facet.put( index, facetValue );
				++index;
			}
		}
	}

	@Indexed
	private static class NumberArrayFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		@org.hibernate.search.annotations.Facet
		private int[] facet;

		private NumberArrayFacetEntity(Long id, int... facet) {
			super();
			this.id = id;
			this.facet = facet;
		}
	}

	@Indexed
	private static class NumberCollectionFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		@org.hibernate.search.annotations.Facet
		private Collection<Float> facet;

		private NumberCollectionFacetEntity(Long id, Float... facet) {
			super();
			this.id = id;
			this.facet = Arrays.asList( facet );
		}
	}

	@Indexed
	private static class NumberMapFacetEntity {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO, index = Index.NO)
		@org.hibernate.search.annotations.Facet
		private Map<Integer, Float> facet;

		private NumberMapFacetEntity(Long id, Float... facet) {
			super();
			this.id = id;
			this.facet = new TreeMap<>();
			int index = 0;
			for ( Float facetValue : facet ) {
				this.facet.put( index, facetValue );
				++index;
			}
		}
	}
}
