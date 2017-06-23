/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.dsl.DSLTest.MappingFactory;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Guillaume Smet
 */
public class SimpleQueryStringDSLTest {
	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Coffee.class, CoffeeBrand.class, Book.class )
			.withProperty( Environment.MODEL_MAPPING, MappingFactory.class.getName() );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		indexTestData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testSimpleQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "balanced arabica" )
				.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesExactlyIds( "Dulsão do Brasil", "Kazaar", "Livanto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "-balanced arabica" )
				.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesExactlyIds( "Bukeela ka Ethiopia", "Linizio Lungo", "Volluto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "powerful \"fruity note\"" )
				.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesExactlyIds( "Ristretto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.matching( "sweet robust" )
				.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesExactlyIds( "Caramelito", "Dulsão do Brasil", "Roma", "Volluto" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testBoost() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
			.onFields( "name", "summary" ).boostedTo( 5f )
			.andField( "description" )
			.withAndAsDefaultOperator()
			.matching( "fruity arabicas south american" )
			.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Decaffeinato", "Ristretto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary" )
				.andField( "description" ).boostedTo( 10f )
				.withAndAsDefaultOperator()
				.matching( "fruity arabicas south american" )
				.createQuery();
		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Ristretto", "Decaffeinato" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testFuzzy() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
			.onFields( "name", "summary", "description" )
			.withAndAsDefaultOperator()
			.matching( "fruity arabica~2" )
			.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesExactlyIds( "Decaffeinato", "Ristretto", "Rosabaya de Colombia", "Volluto" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testAnalyzer() {
		QueryBuilder qb = getBookQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "Molière" )
				.createQuery();

		helper.assertThat( query ).from( Book.class )
				.sort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) )
				.matchesExactlyIds( "Le Grand Molière illustré", "Tartuffe" );

		query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "deplacait" )
				.createQuery();

		helper.assertThat( query ).from( Book.class )
				.sort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) )
				.matchesExactlyIds( "Le chat qui déplaçait des montagnes" );

		qb = sfHolder.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.overridesForField( "author", "titleAnalyzer" )
				.get();
		query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "Molière" )
				.createQuery();

		helper.assertThat( query ).from( Book.class )
				.sort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) )
				.matchesExactlyIds( "Dom Garcie de Navarre", "Le Grand Molière illustré" );
	}
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	public void testNullQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000334" );
		thrown.expectMessage( "does not support null queries" );

		qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( null )
				.createQuery();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	public void testEmptyQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "" )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesNone();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	public void testBlankQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "   " )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesNone();

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "() (())" )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( new SortField( Coffee.NAME_SORT, Type.STRING ) ) )
				.matchesNone();
	}

	private QueryBuilder getCoffeeQueryBuilder() {
		return helper.queryBuilder( Coffee.class );
	}

	private QueryBuilder getBookQueryBuilder() {
		return helper.queryBuilder( Book.class );
	}

	private void indexTestData() {
		CoffeeBrand brandPony = new CoffeeBrand();
		brandPony.setId( 0 );
		brandPony.setName( "My little pony" );
		brandPony.setDescription( "Sells goods for horseback riding and good coffee blends" );

		CoffeeBrand brandMonkey = new CoffeeBrand();
		brandMonkey.setId( 1 );
		brandMonkey.setName( "Monkey Monkey Do" );
		brandPony.setDescription(
				"Offers mover services via monkeys instead of trucks for difficult terrains. Coffees from this brand make monkeys work much faster."
		);

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

		helper.add(
				new Book( "Le chat qui regardait les étoiles", "Lilian Jackson Braun" ),
				new Book( "Le chat qui déplaçait des montagnes", "Lilian Jackson Braun" ) ,
				new Book( "Le Grand Molière illustré", "Caroline Guillot" ),
				new Book( "Tartuffe", "Molière" ),
				new Book( "Dom Garcie de Navarre", "moliere" ) // Molière all lowercase and without an accent
		);
	}

	private void createCoffee(String title, String summary, String description, int intensity, CoffeeBrand brand) {
		Coffee coffee = new Coffee();
		coffee.setId( title );
		coffee.setName( title );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		coffee.setBrand( brand );
		helper.add( coffee );
	}
}
