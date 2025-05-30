/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.query.facet.RangeFacet;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.MigrationHelperLog;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * Simulate a web-shop with basic search which can be refined by facet requests.
 *
 * @author Hardy Ferentschik
 */
class WebShopTest extends AbstractFacetTest {

	private static final MigrationHelperLog log = LoggerFactory.make( MigrationHelperLog.class, MethodHandles.lookup() );

	@Test
	void testSimulateClient() {
		// get hold of the search service
		SearchService searchService = new SearchService( getSessionFactory() );

		// execute the search and display main query results
		List<Car> cars = searchService.searchCar( "BMW" );
		assertThat( cars ).as( "We should have matching cars" ).hasSize( 12 );

		// get the menu items for faceting
		Map<String, List<FacetMenuItem>> facetMenuItems = searchService.getMenuItems();

		List<FacetMenuItem> colorMenuItems = facetMenuItems.get( SearchService.colorFacetName );
		assertThat( colorMenuItems ).as( "Wrong number of menu entries" ).hasSize( 4 );
		for ( FacetMenuItem item : colorMenuItems ) {
			assertThat( item.isSelected() ).isFalse();
		}

		List<FacetMenuItem> ccsMenuItems = facetMenuItems.get( SearchService.cubicCapacityFacetName );
		assertThat( ccsMenuItems ).as( "Wrong number of menu entries" ).hasSize( 3 );
		for ( FacetMenuItem item : ccsMenuItems ) {
			assertThat( item.isSelected() ).isFalse();
		}

		// let the user select a facet menu
		FacetMenuItem selectedItem = facetMenuItems.get( SearchService.colorFacetName ).get( 0 );
		assertThat( selectedItem.getCount() ).as( "Wrong facet count" ).isEqualTo( 3 );

		cars = searchService.selectMenuItem( selectedItem );
		assertThat( cars ).as( "We should have matching cars" ).hasSize( 3 );

		// get the new menu items
		facetMenuItems = searchService.getMenuItems();

		colorMenuItems = facetMenuItems.get( SearchService.colorFacetName );
		assertThat( colorMenuItems ).as( "Wrong number of menu entries" ).hasSize( 1 );
		FacetMenuItem menuItem = colorMenuItems.get( 0 );
		assertThat( menuItem.getCount() ).as( "Wrong facet count" ).isEqualTo( 3 );
		assertThat( menuItem.isSelected() ).isTrue();

		ccsMenuItems = facetMenuItems.get( SearchService.cubicCapacityFacetName );
		assertThat( ccsMenuItems ).as( "Wrong number of menu entries" ).hasSize( 3 );
		for ( FacetMenuItem item : ccsMenuItems ) {
			assertThat( item.isSelected() ).isFalse();
		}

		// deselect the menuitem again
		cars = searchService.deSelectMenuItem( menuItem );
		assertThat( cars ).as( "We should have matching cars" ).hasSize( 12 );
	}


	@Override
	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		List<Car> allCars = new ArrayList<>();
		for ( String make : makes ) {
			for ( String color : colors ) {
				for ( int cc : ccs ) {
					Car car = new Car( make, color, cc );
					session.persist( car );
					allCars.add( car );
				}
			}
		}
		log.infof( "Indexed cars: %s", allCars );
		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
		};
	}

	public static class SearchService {
		public static final String colorFacetName = "color";
		public static final String cubicCapacityFacetName = "cubicCapacity_Numeric";
		private final SessionFactory factory;
		private FullTextQuery currentFullTextQuery;
		private Map<String, List<FacetMenuItem>> menuItems;
		private String queryString;
		private final List<Facet> selectedFacets = new ArrayList<>();

		public SearchService(SessionFactory factory) {
			this.factory = factory;
		}

		/**
		 * Service method for a potential car search website
		 *
		 * @param queryString the query string specified by the user
		 *
		 * @return a list of cars matching the query
		 */
		private List<Car> searchCar(String queryString) {
			this.queryString = queryString;
			FullTextSession fullTextSession = Search.getFullTextSession( factory.openSession() );
			buildFullTextQuery( queryString, fullTextSession );

			Transaction tx = fullTextSession.beginTransaction();
			List<Car> cars = currentFullTextQuery.list();
			tx.commit();

			fullTextSession.close();

			return cars;
		}

		private void buildFullTextQuery(String queryString, FullTextSession fullTextSession) {
			// get a query builder
			final QueryBuilder builder = fullTextSession.getSearchFactory()
					.buildQueryBuilder()
					.forEntity( Car.class )
					.get();

			// build a Lucene query
			final Query makeQuery = builder.keyword().onField( "make" ).matching( queryString ).createQuery();
			BooleanJunction junction = builder.bool();
			junction.must( makeQuery );
			for ( Facet selectedFacet : selectedFacets ) {
				switch ( selectedFacet.getFacetingName() ) {
					case colorFacetName:
						// Clearly not a great user experience, but Search 6 returns a Range<T>
						// which is much easier to work with.
						String value = selectedFacet.getValue();
						junction.filteredBy( builder.keyword()
								.onField( "color" )
								.matching( value )
								.createQuery() );
						break;
					case cubicCapacityFacetName:
						// Clearly not a great user experience, but Search 6 returns a Range<T>
						// which is much easier to work with.
						Integer min = ( (RangeFacet<Integer>) selectedFacet ).getMin();
						Integer max = ( (RangeFacet<Integer>) selectedFacet ).getMax();
						junction.filteredBy( builder.range()
								.onField( Car.CUBIC_CAPACITY_STRING_FACET_NUMERIC_ENCODING )
								.from( min ).to( max )
								.createQuery() );
						break;
					default:
						throw new IllegalStateException( "Unexpected value: " + selectedFacet.getFacetingName() );
				}
			}
			final Query query = junction.createQuery();

			// create facets for navigation
			// discrete faceting
			final FacetingRequest colorFacet = builder.facet()
					.name( colorFacetName )
					.onField( "color" )
					.discrete()
					.createFacetingRequest();
			// range faceting
			final FacetingRequest cubicCapacityFacet = builder.facet()
					.name( cubicCapacityFacetName )
					.onField( Car.CUBIC_CAPACITY_STRING_FACET_NUMERIC_ENCODING )
					.range()
					.below( 2500 ).excludeLimit()
					.from( 2500 ).to( 3000 )
					.above( 3000 ).excludeLimit()
					.createFacetingRequest();


			// create the fulltext query, enable the facets via the facet manager and return the results
			currentFullTextQuery = fullTextSession.createFullTextQuery( query, Car.class );
			currentFullTextQuery.setFirstResult( 0 ).setMaxResults( 20 );
			currentFullTextQuery.getFacetManager().enableFaceting( colorFacet ).enableFaceting( cubicCapacityFacet );
		}

		public Map<String, List<FacetMenuItem>> getMenuItems() {
			menuItems = new HashMap<>();
			List<FacetMenuItem> items = new ArrayList<>();

			int i = 0;
			for ( Facet facet : currentFullTextQuery.getFacetManager().getFacets( colorFacetName ) ) {
				items.add( new FacetMenuItem( facet, selectedFacets.contains( facet ), i ) );
				i++;
			}
			menuItems.put( colorFacetName, items );

			items = new ArrayList<>();
			i = 0;
			for ( Facet facet : currentFullTextQuery.getFacetManager().getFacets( cubicCapacityFacetName ) ) {
				items.add( new FacetMenuItem( facet, selectedFacets.contains( facet ), i++ ) );
			}
			menuItems.put( cubicCapacityFacetName, items );
			return menuItems;
		}

		public List<Car> selectMenuItem(FacetMenuItem item) {
			// find the right facet using the facet manager
			List<Facet> facets = currentFullTextQuery.getFacetManager().getFacets( item.getFacetingName() );
			Facet facet = facets.get( item.getIndex() );
			selectedFacets.add( facet );

			// rer-run the query
			FullTextSession fullTextSession = Search.getFullTextSession( factory.openSession() );
			buildFullTextQuery( queryString, fullTextSession );

			Transaction tx = fullTextSession.beginTransaction();
			List<Car> cars = currentFullTextQuery.list();
			tx.commit();
			fullTextSession.close();
			return cars;
		}

		public List<Car> deSelectMenuItem(FacetMenuItem item) {
			// find the right facet using the facet manager
			List<Facet> facets = currentFullTextQuery.getFacetManager().getFacets( item.getFacetingName() );
			Facet facet = facets.get( item.getIndex() );
			selectedFacets.remove( facet );

			// rer-run the query
			FullTextSession fullTextSession = Search.getFullTextSession( factory.openSession() );
			buildFullTextQuery( queryString, fullTextSession );

			Transaction tx = fullTextSession.beginTransaction();
			List<Car> cars = currentFullTextQuery.list();
			tx.commit();
			fullTextSession.close();
			return cars;
		}

	}

	// simulate some sort of frontend data structure for displaying the facet menu
	static class FacetMenuItem {
		private final boolean isSelected;
		private final int index;
		private final String facetingName;
		private final Facet facet;

		public FacetMenuItem(Facet facet, boolean selected, int index) {
			this.isSelected = selected;
			this.facet = facet;
			this.facetingName = facet.getFacetingName();
			this.index = index;
		}

		public boolean isSelected() {
			return isSelected;
		}

		public int getCount() {
			return facet.getCount();
		}

		public String getValue() {
			String value;
			if ( facet instanceof RangeFacet ) {
				// this could get more involved
				RangeFacet rangeFacet = (RangeFacet) facet;
				value = rangeFacet.getMin().toString() + " - " + rangeFacet.getMax().toString();
			}
			else {
				value = facet.getValue();
			}
			return value;
		}

		public int getIndex() {
			return index;
		}

		public String getFacetingName() {
			return facetingName;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "FacetMenuItem" );
			sb.append( "{isSelected=" ).append( isSelected );
			sb.append( ", facet=" ).append( facet );
			sb.append( '}' );
			return sb.toString();
		}
	}
}
