/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class SimpleQueryStringDSLTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@SuppressWarnings({ "unchecked" })
	public void testSimpleQueryString() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			QueryBuilder qb = getCoffeeQueryBuilder();

			Query query = qb.simpleQueryString()
					.onFields( "name", "summary", "description" )
					.withAndAsDefaultOperator()
					.matching( "balanced arabica" )
					.createQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) );
			List<Coffee> results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Dulsão do Brasil", "Kazaar", "Livanto" ), results );

			query = qb.simpleQueryString()
					.onFields( "name", "summary", "description" )
					.withAndAsDefaultOperator()
					.matching( "-balanced arabica" )
					.createQuery();
			fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) );
			results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Bukeela ka Ethiopia", "Linizio Lungo", "Volluto" ), results );

			query = qb.simpleQueryString()
					.onFields( "name", "summary", "description" )
					.withAndAsDefaultOperator()
					.matching( "powerful \"fruity note\"" )
					.createQuery();
			fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) );
			results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Ristretto" ), results );

			query = qb.simpleQueryString()
					.onFields( "name", "summary", "description" )
					.matching( "sweet robust" )
					.createQuery();
			fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) );
			results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Caramelito", "Dulsão do Brasil", "Roma", "Volluto" ), results );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@SuppressWarnings({ "unchecked" })
	public void testBoost() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			QueryBuilder qb = getCoffeeQueryBuilder();

			Query query = qb.simpleQueryString()
				.onFields( "name", "summary" ).boostedTo( 5f )
				.andField( "description" )
				.withAndAsDefaultOperator()
				.matching( "fruity arabicas south american" )
				.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( SortField.FIELD_SCORE ) );
			List<Coffee> results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Decaffeinato", "Ristretto" ), results );

			query = qb.simpleQueryString()
					.onFields( "name", "summary" )
					.andField( "description" ).boostedTo( 10f )
					.withAndAsDefaultOperator()
					.matching( "fruity arabicas south american" )
					.createQuery();
			fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( SortField.FIELD_SCORE ) );
			results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Ristretto", "Decaffeinato" ), results );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@SuppressWarnings({ "unchecked" })
	public void testFuzzy() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			QueryBuilder qb = getCoffeeQueryBuilder();

			Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "fruity arabica~2" )
				.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Coffee.class );
			fullTextQuery.setSort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) );
			List<Coffee> results = fullTextQuery.getResultList();

			compareCoffeeResultsAndExpected( Arrays.asList( "Decaffeinato", "Ristretto", "Rosabaya de Colombia", "Volluto" ), results );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@SuppressWarnings({ "unchecked" })
	public void testAnalyzer() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			QueryBuilder qb = getBookQueryBuilder();

			Query query = qb.simpleQueryString()
					.onFields( "title", "author" )
					.withAndAsDefaultOperator()
					.matching( "Molière" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Book.class );
			fullTextQuery.setSort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) );
			List<Book> results = fullTextQuery.getResultList();

			compareBookResultsAndExpected( Arrays.asList( "Le Grand Molière illustré", "Tartuffe" ), results );

			query = qb.simpleQueryString()
					.onFields( "title", "author" )
					.withAndAsDefaultOperator()
					.matching( "deplacait" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Book.class );
			fullTextQuery.setSort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) );
			results = fullTextQuery.getResultList();

			compareBookResultsAndExpected( Arrays.asList( "Le chat qui déplaçait des montagnes" ), results );

			qb = fullTextSession.getSearchFactory()
					.buildQueryBuilder()
					.forEntity( Book.class )
					.overridesForField( "author", "titleAnalyzer" )
					.get();
			query = qb.simpleQueryString()
					.onFields( "title", "author" )
					.withAndAsDefaultOperator()
					.matching( "Molière" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Book.class );
			fullTextQuery.setSort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) );
			results = fullTextQuery.getResultList();

			compareBookResultsAndExpected( Arrays.asList( "Dom Garcie de Navarre", "Le Grand Molière illustré" ), results );
		}
		finally {
			transaction.commit();
		}
	}

	private void compareCoffeeResultsAndExpected(List<String> expected, List<Coffee> results) {
		assertThat( results ).onProperty( "name" ).isEqualTo( expected );
	}

	private void compareBookResultsAndExpected(List<String> expected, List<Book> results) {
		assertThat( results ).onProperty( "title" ).isEqualTo( expected );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Coffee.class,
				CoffeeBrand.class,
				Book.class
		};
	}

	private QueryBuilder getCoffeeQueryBuilder() {
		return fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Coffee.class )
				.get();
	}

	private QueryBuilder getBookQueryBuilder() {
		return fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get();
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
				"Mild and well-rounded on the palate, Linizio Lungo is a blend of fine Arabica enhancing malt and cereal notes.",
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

		fullTextSession.persist( new Book( 1L, "Le chat qui regardait les étoiles", "Lilian Jackson Braun" ) );
		fullTextSession.persist( new Book( 2L, "Le chat qui déplaçait des montagnes", "Lilian Jackson Braun" ) );
		fullTextSession.persist( new Book( 3L, "Le Grand Molière illustré", "Caroline Guillot" ) );
		fullTextSession.persist( new Book( 4L, "Tartuffe", "Molière" ) );
		fullTextSession.persist( new Book( 5L, "Dom Garcie de Navarre", "moliere" ) ); // Molière all lowercase and without an accent

		tx.commit();
		fullTextSession.clear();
	}

	private void createCoffee(String title, String summary, String description, int intensity, CoffeeBrand brand) {
		Coffee coffee = new Coffee();
		coffee.setName( title );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		coffee.setBrand( brand );
		fullTextSession.persist( coffee );
	}
}
