/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.fest.assertions.Condition;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class MoreLikeThisTest extends SearchTestBase {
	private static final Log log = LoggerFactory.make();

	private FullTextSession fullTextSession;
	private boolean outputLogs = false;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMoreLikeThisBasicBehavior() {
		Transaction transaction = fullTextSession.beginTransaction();
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			Query mltQuery = qb
					.moreLikeThis()
					.favorSignificantTermsWithFactor( 1 )
					.comparingAllFields()
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			assertThat( results ).isNotEmpty();

			Set<Term> terms = new HashSet<Term>( 100 );
			mltQuery.extractTerms( terms );
			assertThat( terms )
					.describedAs( "internalDescription should be ignored" )
					.doesNotSatisfy(
							new Condition<Collection<?>>() {
								@Override
								public boolean matches(Collection<?> value) {
									for ( Term term : (Collection<Term>) value ) {
										if ( "internalDescription".equals( term.field() ) ) {
											return true;
										}
									}
									return false;
								}
							}
					);
			outputQueryAndResults( decaffInstance, mltQuery, results );

			//custom fields
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			assertThat( results ).isNotEmpty();
			assertThat( mltQuery instanceof BooleanQuery );
			BooleanQuery topMltQuery = (BooleanQuery) mltQuery;
			// FIXME: I'd prefer a test that uses data instead of how the query is actually built
			assertThat( topMltQuery.getClauses() ).onProperty( "query.boost" ).contains( 1f, 10f );

			outputQueryAndResults( decaffInstance, mltQuery, results );

			//using non compatible field
			try {
				qb
						.moreLikeThis()
						.comparingField( "summary" )
						.andField( "internalDescription" )
						.toEntityWithId( decaffInstance.getId() )
						.createQuery();
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.as( "Internal description is neither stored nor store termvectors" )
						.contains( "internalDescription" );
			}
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMoreLikeThisToEntity() {
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// query results to compare toEntity() results against
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// pass entity itself in a managed state
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( decaffInstance )
					.createQuery();
			List<Object[]> entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( decaffInstance, mltQuery, entityResults );

			// pass entity itself with a matching id but different values
			// the id should take precedene
			Coffee nonMatchingOne = (Coffee) results.get( results.size() - 1 )[0];
			Coffee copyOfDecaffInstance = new Coffee();
			copyOfDecaffInstance.setId( decaffInstance.getId() );
			copyOfDecaffInstance.setInternalDescription( nonMatchingOne.getInternalDescription() );
			copyOfDecaffInstance.setName( nonMatchingOne.getName() );
			copyOfDecaffInstance.setDescription( nonMatchingOne.getDescription() );
			copyOfDecaffInstance.setIntensity( nonMatchingOne.getIntensity() );
			copyOfDecaffInstance.setSummary( nonMatchingOne.getSummary() );
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( copyOfDecaffInstance )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( decaffInstance, mltQuery, entityResults );

			// pass entity itself with the right values but no id
			copyOfDecaffInstance = new Coffee();
			copyOfDecaffInstance.setInternalDescription( decaffInstance.getInternalDescription() );
			copyOfDecaffInstance.setName( decaffInstance.getName() );
			copyOfDecaffInstance.setDescription( decaffInstance.getDescription() );
			copyOfDecaffInstance.setIntensity( decaffInstance.getIntensity() );
			copyOfDecaffInstance.setSummary( decaffInstance.getSummary() );
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( copyOfDecaffInstance )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( decaffInstance, mltQuery, entityResults );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMoreLikeThisExcludingEntityBeingCompared() {
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> results;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );

			// exclude comparing entity
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			mltQuery = qb
					.moreLikeThis()
					.excludeEntityUsedForComparison()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> resultsWoComparingEntity = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( resultsWoComparingEntity ).hasSize( results.size() - 1 );
			for ( int index = 0; index < resultsWoComparingEntity.size(); index++ ) {
				Object[] real = resultsWoComparingEntity.get( index );
				Object[] expected = results.get( index + 1 );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}
			outputQueryAndResults( decaffInstance, mltQuery, resultsWoComparingEntity );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMoreLikeThisOnCompressedFields() {
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> entityResults;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// using compressed field
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "brand.description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( entityResults ).isNotEmpty();
			Long matchingElements = (Long) fullTextSession.createQuery(
					"select count(*) from Coffee c where c.brand.name like '%pony'"
			).uniqueResult();
			assertThat( entityResults ).hasSize( matchingElements.intValue() );
			float score = -1;
			for ( Object[] element : entityResults ) {
				if ( score == -1 ) {
					score = (Float) element[1];
				}
				assertThat( element[1] ).as( "All scores should be equal as the same brand is used" )
						.isEqualTo( score );
			}
			outputQueryAndResults( decaffInstance, mltQuery, entityResults );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMoreLikeThisOnEmbeddedFields() {
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> entityResults;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// using fields from IndexedEmbedded
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "brand.name" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( entityResults ).isNotEmpty();
			Long matchingElements = (Long) fullTextSession.createQuery(
					"select count(*) from Coffee c where c.brand.name like '%pony'"
			).uniqueResult();
			assertThat( entityResults ).hasSize( matchingElements.intValue() );
			float score = -1;
			for ( Object[] element : entityResults ) {
				if ( score == -1 ) {
					score = (Float) element[1];
				}
				assertThat( element[1] ).as( "All scores should be equal as the same brand is used" )
						.isEqualTo( score );
			}
			outputQueryAndResults( decaffInstance, mltQuery, entityResults );

			// using indexed embedded id from document
			try {
				qb
						.moreLikeThis()
						.comparingField( "brand.id" )
						.toEntityWithId( decaffInstance.getId() )
						.createQuery();
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.as( "Field cannot be used" )
						.contains( "brand.id" );
			}
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1614")
	public void testMoreLikeThisOnUnknownFieldThrowsException() {
		QueryBuilder queryBuilder = getCoffeeQueryBuilder();
		Coffee decaffInstance = getDecaffInstance( queryBuilder );

		try {
			queryBuilder.moreLikeThis()
					.comparingField( "foo" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			fail( "Creating the query should have failed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000218" ) );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Coffee.class,
				CoffeeBrand.class
		};
	}

	private QueryBuilder getCoffeeQueryBuilder() {
		return fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Coffee.class )
				.get();
	}

	private Coffee getDecaffInstance(QueryBuilder qb) {
		Query decaff = qb.keyword().onField( "name" ).matching( "Decaffeinato" ).createQuery();
		return (Coffee) fullTextSession.createFullTextQuery( decaff, Coffee.class )
				.list()
				.get( 0 );
	}

	private void outputQueryAndResults(Coffee originalInstance, Query mltQuery, List<Object[]> results) {
		// set to true to display results
		if ( outputLogs ) {
			StringBuilder builder = new StringBuilder( "Initial coffee: " )
					.append( originalInstance ).append( "\n\n" )
					.append( "Query: " ).append( mltQuery.toString() ).append( "\n\n" )
					.append( "Matching coffees" ).append( "\n" );
			for ( Object[] entry : results ) {
				builder.append( "    Score: " ).append( entry[1] );
				builder.append( " | Coffee: " ).append( entry[0] ).append( "\n" );
			}
			log.debug( builder.toString() );
		}
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();

		CoffeeBrand brandPony = new CoffeeBrand();
		brandPony.setName( "My little pony" );
		brandPony.setDescription( "Sells goods for horseback riding and good coffee blends" );
		fullTextSession.persist( brandPony );
		CoffeeBrand brandMonkey = new CoffeeBrand();
		brandMonkey.setName( "Monkey Monkey Do" );
		brandPony.setDescription(
				"Offers mover services via monkeys instead of trucks for difficult terrains. Coffees from this brand make monkeys work much faster."
		);
		fullTextSession.persist( brandMonkey );
		createCoffee(
				"Kazaar",
				"EXCEPTIONALLY INTENSE AND SYRUPY",
				"A daring blend of two Robustas from Brazil and Guatemala, specially prepared for Nespresso, and a separately roasted Arabica from South America, Kazaar is a coffee of exceptional intensity. Its powerful bitterness and notes of pepper are balanced by a full and creamy texture.",
				12,
				brandMonkey
		);
		createCoffee(
				"Dharkan",
				"LONG ROASTED AND VELVETY",
				"This blend of Arabicas from Latin America and Asia fully unveils its character thanks to the technique of long roasting at a low temperature. Its powerful personality reveals intense roasted notes together with hints of bitter cocoa powder and toasted cereals that express themselves in a silky and velvety txture.",
				11,
				brandPony
		);
		createCoffee(
				"Ristretto",
				"POWERFUL AND CONTRASTING",
				"A blend of South American and East African Arabicas, with a touch of Robusta, roasted separately to create the subtle fruity note of this full-bodied, intense espresso.",
				10,
				brandMonkey
		);
		createCoffee(
				"Arpeggio",
				"INTENSE AND CREAMY",
				"A dark roast of pure South and Central American Arabicas, Arpeggio has a strong character and intense body, enhanced by cocoa notes.",
				9,
				brandPony
		);
		createCoffee(
				"Roma",
				"FULL AND BALANCED",
				"The balance of lightly roasted South and Central American Arabicas with Robusta, gives Roma sweet and woody notes and a full, lasting taste on the palate.",
				8,
				brandMonkey
		);
		createCoffee(
				"Livanto",
				"ROUND AND BALANCED",
				"A pure Arabica from South and Central America, Livanto is a well-balanced espresso characterised by a roasted caramelised note.",
				6,
				brandMonkey
		);
		createCoffee(
				"Capriccio",
				"RICH AND DISTINCTIVE",
				"Blending South American Arabicas with a touch of Robusta, Capriccio is an espresso with a rich aroma and a strong typical cereal note.",
				5,
				brandMonkey
		);
		createCoffee(
				"Volluto",
				"SWEET AND LIGHT",
				"A pure and lightly roasted Arabica from South America, Volluto reveals sweet and biscuity flavours, reinforced by a little acidity and a fruity note.",
				4,
				brandMonkey
		);
		createCoffee(
				"Cosi",
				"LIGHT AND LEMONY",
				"Pure, lightly roasted East African, Central and South American Arabicas make Cosi a light-bodied espresso with refreshing citrus notes.",
				3,
				brandMonkey
		);
		createCoffee(
				"Indriya from India",
				"POWERFUL AND SPICY",
				"Indriya from India is the noble marriage of Arabicas with a hint of Robusta from southern India. It is a full-bodied espresso, which has a distinct personality with notes of spices.",
				10,
				brandMonkey
		);
		createCoffee(
				"Rosabaya de Colombia",
				"FRUITY AND BALANCED",
				"This blend of fine, individually roasted Colombian Arabicas, develops a subtle acidity with typical red fruit and winey notes.",
				6,
				brandMonkey
		);
		createCoffee(
				"Dulsão do Brasil",
				"SWEET AND SATINY SMOOTH",
				"A pure Arabica coffee, Dulsão do Brasil is a delicate blend of red and yellow Bourbon beans from Brazil. Its satiny smooth, elegantly balanced flavor is enhanced with a note of delicately toasted grain.",
				4,
				brandMonkey
		);
		createCoffee(
				"Bukeela ka Ethiopia",
				"",
				"This delicate Lungo expresses a floral bouquet reminiscent of jasmine, white lily, bergamot and orange blossom together with notes of wood. A pure Arabica blend composed of two very different coffees coming from the birthplace of coffee, Ethiopia. The blend’s coffees are roasted separately: one portion short and dark to guarantee the body, the other light but longer to preserve the delicate notes.",
				3,
				brandMonkey
		);
		createCoffee(
				"Fortissio Lungo",
				"RICH AND INTENSE",
				"Made from Central and South American Arabicas with just a hint of Robusta, Fortissio Lungo is an intense full-bodied blend with bitterness, which develops notes of dark roasted beans.",
				7,
				brandMonkey
		);
		createCoffee(
				"Vivalto Lungo",
				"COMPLEX AND BALANCED",
				"Vivalto Lungo is a balanced coffee made from a complex blend of separately roasted South American and East African Arabicas, combining roasted and subtle floral notes.",
				4,
				brandMonkey
		);
		createCoffee(
				"Linizio Lungo",
				"ROUND AND SMOOTH",
				"Mild and well-rounded on the palate, Linizio Lungo is a blend of fine Arabicas enhancing malt and cereal notes.",
				4,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato Intenso",
				"DENSE AND POWERFUL",
				"Dark roasted South American Arabicas with a touch of Robusta bring out the subtle cocoa and roasted cereal notes of this full-bodied decaffeinated espresso.",
				7,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato Lungo",
				"LIGHT AND FULL-FLAVOURED",
				"The slow roasting of this blend of South American Arabicas with a touch of Robusta gives Decaffeinato Lungo a smooth, creamy body and roasted cereal flavour.",
				3,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato",
				"FRUITY AND DELICATE",
				"A blend of South American Arabicas reinforced with just a touch of Robusta is lightly roasted to reveal an aroma of red fruit.",
				2,
				brandPony
		);
		createCoffee(
				"Caramelito",
				"CARAMEL FLAVOURED",
				"The sweet flavour of caramel softens the roasted notes of the Livanto Grand Cru. This delicate gourmet marriage evokes the creaminess of soft toffee.",
				6,
				brandMonkey
		);
		createCoffee(
				"Ciocattino",
				"CHOCOLATE FLAVOURED",
				"Dark and bitter chocolate notes meet the caramelized roast of the Livanto Grand Cru. A rich combination reminiscent of a square of dark chocolate.",
				6,
				brandMonkey
		);
		createCoffee(
				"Vanilio",
				"VANILLA FLAVOURED",
				"A balanced harmony between the rich and the velvety aromas of vanilla and the mellow flavour of the Livanto Grand Cru. A blend distinguished by its full flavour, infinitely smooth and silky on the palate.",
				6,
				brandMonkey
		);

		tx.commit();
		fullTextSession.clear();
	}

	private void createCoffee(String title, String summary, String description, int intensity, CoffeeBrand brand) {
		Coffee coffee = new Coffee();
		coffee.setName( title );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		coffee.setInternalDescription(
				"Same internal description of coffee and blend that would make things look quite the same."
		);
		coffee.setBrand( brand );
		fullTextSession.persist( coffee );
	}
}
