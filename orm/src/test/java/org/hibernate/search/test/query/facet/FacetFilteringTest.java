/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetCombine;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.testing.TestForIssue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class FacetFilteringTest extends AbstractFacetTest {

	@Test
	public void testDiscreteFacetDrillDown() throws Exception {
		final String facetName = "ccs";
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();

		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( request );
		query.setFirstResult( 0 ).setMaxResults( 1 );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );

		List<Facet> facetList = facetManager.getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );

		facetManager.getFacetGroup( facetName ).selectFacets( facetList.get( 0 ) );
		query.list();
		assertEquals( "Wrong number of query matches", 5, query.getResultSize() );
		List<Facet> newFacetList = facetManager.getFacets( facetName );
		assertFacetCounts( newFacetList, new int[] { 5, 0, 0, 0 } );

		facetManager.getFacetGroup( facetName ).selectFacets( facetList.get( 1 ) );
		query.setMaxResults( 1000 ); // When testing against Elasticsearch you need to stay under the maximum page limit
		assertEquals( "Wrong number of query matches", 9, query.list().size() );
		newFacetList = facetManager.getFacets( facetName );
		assertFacetCounts( newFacetList, new int[] { 5, 4, 0, 0 } );
	}

	@Test
	public void testMultipleFacetDrillDown() throws Exception {
		final String ccsFacetName = "ccs";
		FacetingRequest ccsFacetRequest = queryBuilder( Car.class ).facet()
				.name( ccsFacetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();

		final String colorFacetName = "color";
		final String colorFacetFieldName = "color";
		FacetingRequest colorFacetRequest = queryBuilder( Car.class ).facet()
				.name( colorFacetName )
				.onField( colorFacetFieldName )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();

		FullTextQuery query = createMatchAllQuery( Car.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( colorFacetRequest );
		facetManager.enableFaceting( ccsFacetRequest );
		assertEquals( "Wrong number of query matches", 50, query.getResultSize() );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 12, 12, 12, 2 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 17, 16, 16, 1 } );

		Facet colorFacet = facetManager.getFacets( colorFacetName ).get( 0 );
		facetManager.getFacetGroup( colorFacetName ).selectFacets( colorFacet );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 0, 0, 0, 0 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 4, 4, 4, 0 } );

		Facet ccsFacet = facetManager.getFacets( ccsFacetName ).get( 0 );
		facetManager.getFacetGroup( colorFacetName ).selectFacets( colorFacet );
		facetManager.getFacetGroup( ccsFacetName ).selectFacets( ccsFacet );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 4, 0, 0, 0, 0 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 4, 0, 0, 0 } );

		assertEquals(
				"Facets should not take count in equality",
				colorFacet,
				facetManager.getFacets( colorFacetName ).get( 0 )
		);
		assertTrue(
				"We should be able to find facets amongst the selected ones",
				facetManager.getFacetGroup( colorFacetName ).getSelectedFacets().contains(
						facetManager.getFacets( colorFacetName ).get( 0 )
				)
		);

		facetManager.getFacetGroup( colorFacetName ).clearSelectedFacets();
		facetManager.getFacetGroup( ccsFacetName ).clearSelectedFacets();
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 12, 12, 12, 2 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 17, 16, 16, 1 } );
	}

	@Test
	public void testRangeFacetDrillDown() {
		final String indexFieldName = "price";
		final String priceRange = "priceRange";
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Fruit.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		assertEquals( "Wrong number of query matches", 10, query.getResultSize() );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );

		facetManager.getFacetGroup( priceRange ).selectFacets( facets.get( 2 ) );

		assertEquals( "Wrong number of query matches", 2, query.list().size() );
		List<Facet> newFacetList = facetManager.getFacets( priceRange );
		assertFacetCounts( newFacetList, new int[] { 2, 0, 0, 0 } );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-812")
	public void testDiscreteFacetDrillDownWithMultiFacetSelect() throws Exception {
		final String indexFieldName = "ingredients.name";
		final String facetName = "ingredientsFacetRequest";

		FacetingRequest request = queryBuilder( Recipe.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacetingRequest();

		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Recipe.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( request );

		assertEquals( "Wrong number of query matches", 3, query.getResultSize() );

		List<Facet> facetList = facetManager.getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 3, 2, 1, 1, 1 } );
		assertFacetValues( facetList, new Object[] { "Egg", "Potato", "Milk", "Onion", "Salt" } );

		// use default facet selection via FacetCombine.OR - all should stay the same since there
		// are eggs in all recipts
		facetManager.getFacetGroup( facetName ).selectFacets( facetList.get( 0 ), facetList.get( 3 ) );
		query.list();
		assertEquals( "Wrong number of query matches", 3, query.getResultSize() );
		facetList = facetManager.getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 3, 2, 1, 1, 1 } );
		assertFacetValues( facetList, new Object[] { "Egg", "Potato", "Milk", "Onion", "Salt" } );

		facetManager.getFacetGroup( facetName ).selectFacets( FacetCombine.AND, facetList.get( 0 ), facetList.get( 3 ) );
		query.list();
		assertEquals( "Wrong number of query matches", 1, query.getResultSize() );
		facetList = facetManager.getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 1, 1, 1, 1 } );
		assertFacetValues( facetList, new Object[] { "Egg", "Onion", "Potato", "Salt" } );
	}

	@Override
	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( int i = 0; i < fruits.length; i++ ) {
			Fruit fruit = new Fruit( fruits[i], fruitPrices[i] );
			session.save( fruit );
		}

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

		Ingredient potato = new Ingredient( "Potato" );
		session.save( potato );

		Ingredient milk = new Ingredient( "Milk" );
		session.save( milk );

		Ingredient salt = new Ingredient( "Salt" );
		session.save( salt );

		Ingredient egg = new Ingredient( "Egg" );
		session.save( egg );

		Ingredient onion = new Ingredient( "Onion" );
		session.save( onion );

		Recipe potatoMash = new Recipe( "Potato Mash" );
		potatoMash.addIngredients( potato, milk, egg );
		session.save( potatoMash );

		Recipe tortilla = new Recipe( "Tortilla" );
		tortilla.addIngredients( potato, egg, onion, salt );
		session.save( tortilla );

		Recipe poachedEgg = new Recipe( "Poached Egg" );
		poachedEgg.addIngredients( egg );
		session.save( poachedEgg );

		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Fruit.class,
				Recipe.class,
				Ingredient.class
		};
	}
}
