/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.AnalysisNames;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

/**
 * @author Guillaume Smet
 */
class SimpleQueryStringDSLTest {
	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Coffee.class, Book.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void setUp() {
		indexTestData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testSimpleQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "balanced arabica" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesExactlyIds( "Dulsão do Brasil", "Kazaar", "Livanto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "-balanced arabica" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesExactlyIds( "Bukeela ka Ethiopia", "Linizio Lungo", "Volluto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "powerful \"fruity note\"" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesExactlyIds( "Ristretto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.matching( "sweet robust" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesExactlyIds( "Caramelito", "Dulsão do Brasil", "Roma", "Volluto" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testBoost() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary" ).boostedTo( 5f )
				.andField( "description" )
				.withAndAsDefaultOperator()
				.matching( "fruity arabicas south american" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Decaffeinato", "Ristretto" );

		query = qb.simpleQueryString()
				.onFields( "name", "summary" )
				.andField( "description" ).boostedTo( 10f )
				.withAndAsDefaultOperator()
				.matching( "fruity arabicas south american" )
				.createQuery();
		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Ristretto", "Decaffeinato" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testFuzzy() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "fruity arabica~2" )
				.createQuery();

		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesExactlyIds( "Decaffeinato", "Ristretto", "Rosabaya de Colombia", "Volluto" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testAnalyzer() {
		QueryBuilder qb = getBookQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "Molière" )
				.createQuery();

		helper.assertThatQuery( query ).from( Book.class )
				.sort( qb.sort().byField( "title_sort" ).createSort() )
				.matchesExactlyIds( "Le Grand Molière illustré", "Tartuffe" );

		query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "deplacait" )
				.createQuery();

		helper.assertThatQuery( query ).from( Book.class )
				.sort( qb.sort().byField( "title_sort" ).createSort() )
				.matchesExactlyIds( "Le chat qui déplaçait des montagnes" );

		qb = sfHolder.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.overridesForField( "author", AnalysisNames.ANALYZER_WHITESPACE_LOWERCASE_ASCIIFOLDING )
				.get();
		query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "Molière" )
				.createQuery();

		helper.assertThatQuery( query ).from( Book.class )
				.sort( qb.sort().byField( "title_sort" ).createSort() )
				.matchesExactlyIds( "Dom Garcie de Navarre", "Le Grand Molière illustré" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testNullQueryString() {
		assertThatThrownBy( () -> {
			QueryBuilder qb = getCoffeeQueryBuilder();
			qb.simpleQueryString()
					.onFields( "name", "summary", "description" )
					.withAndAsDefaultOperator()
					.matching( null )
					.createQuery();
		} ).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'queryString' must not be null" );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testEmptyQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "" )
				.createQuery();

		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesNone();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testBlankQueryString() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "   " )
				.createQuery();

		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesNone();

		query = qb.simpleQueryString()
				.onFields( "name", "summary", "description" )
				.withAndAsDefaultOperator()
				.matching( "() (())" )
				.createQuery();

		helper.assertThatQuery( query ).from( Coffee.class )
				.sort( qb.sort().byField( Coffee.NAME_SORT ).createSort() )
				.matchesNone();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3039")
	void testSearchOnEmbeddedObjectId() {
		QueryBuilder qb = getCoffeeQueryBuilder();

		Query query = qb.simpleQueryString()
				.onFields( "maker.id" )
				.matching( "Stable" )
				.createQuery();

		helper.assertThatQuery( query ).from( Coffee.class )
				.matchesUnorderedIds( "Dharkan", "Arpeggio", "Decaffeinato" );
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

		CoffeeMaker makerStable = new CoffeeMaker();
		makerStable.setId( "Stable" );
		makerStable.setName( "Stable Inc." );

		CoffeeMaker makerZoo = new CoffeeMaker();
		makerZoo.setId( "Zoo" );
		makerZoo.setName( "Zoo Limited" );

		createCoffee(
				"Kazaar",
				"EXCEPTIONALLY INTENSE AND SYRUPY",
				"A daring blend of two Robustas from Brazil and Guatemala, specially prepared for Nespresso, and a separately roasted Arabica from South America, Kazaar is a coffee of exceptional intensity. Its powerful bitterness and notes of pepper are balanced by a full and creamy texture.",
				12,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Dharkan",
				"LONG ROASTED AND VELVETY",
				"This blend of Arabicas from Latin America and Asia fully unveils its character thanks to the technique of long roasting at a low temperature. Its powerful personality reveals intense roasted notes together with hints of bitter cocoa powder and toasted cereals that express themselves in a silky and velvety txture.",
				11,
				brandPony,
				makerStable
		);
		createCoffee(
				"Ristretto",
				"POWERFUL AND CONTRASTING",
				"A blend of South American and East African Arabicas, with a touch of Robusta, roasted separately to create the subtle fruity note of this full-bodied, intense espresso.",
				10,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Arpeggio",
				"INTENSE AND CREAMY",
				"A dark roast of pure South and Central American Arabicas, Arpeggio has a strong character and intense body, enhanced by cocoa notes.",
				9,
				brandPony,
				makerStable
		);
		createCoffee(
				"Roma",
				"FULL AND BALANCED",
				"The balance of lightly roasted South and Central American Arabicas with Robusta, gives Roma sweet and woody notes and a full, lasting taste on the palate.",
				8,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Livanto",
				"ROUND AND BALANCED",
				"A pure Arabica from South and Central America, Livanto is a well-balanced espresso characterised by a roasted caramelised note.",
				6,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Capriccio",
				"RICH AND DISTINCTIVE",
				"Blending South American Arabicas with a touch of Robusta, Capriccio is an espresso with a rich aroma and a strong typical cereal note.",
				5,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Volluto",
				"SWEET AND LIGHT",
				"A pure and lightly roasted Arabica from South America, Volluto reveals sweet and biscuity flavours, reinforced by a little acidity and a fruity note.",
				4,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Cosi",
				"LIGHT AND LEMONY",
				"Pure, lightly roasted East African, Central and South American Arabicas make Cosi a light-bodied espresso with refreshing citrus notes.",
				3,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Indriya from India",
				"POWERFUL AND SPICY",
				"Indriya from India is the noble marriage of Arabicas with a hint of Robusta from southern India. It is a full-bodied espresso, which has a distinct personality with notes of spices.",
				10,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Rosabaya de Colombia",
				"FRUITY AND BALANCED",
				"This blend of fine, individually roasted Colombian Arabicas, develops a subtle acidity with typical red fruit and winey notes.",
				6,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Dulsão do Brasil",
				"SWEET AND SATINY SMOOTH",
				"A pure Arabica coffee, Dulsão do Brasil is a delicate blend of red and yellow Bourbon beans from Brazil. Its satiny smooth, elegantly balanced flavor is enhanced with a note of delicately toasted grain.",
				4,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Bukeela ka Ethiopia",
				"",
				"This delicate Lungo expresses a floral bouquet reminiscent of jasmine, white lily, bergamot and orange blossom together with notes of wood. A pure Arabica blend composed of two very different coffees coming from the birthplace of coffee, Ethiopia. The blend’s coffees are roasted separately: one portion short and dark to guarantee the body, the other light but longer to preserve the delicate notes.",
				3,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Fortissio Lungo",
				"RICH AND INTENSE",
				"Made from Central and South American Arabicas with just a hint of Robusta, Fortissio Lungo is an intense full-bodied blend with bitterness, which develops notes of dark roasted beans.",
				7,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Vivalto Lungo",
				"COMPLEX AND BALANCED",
				"Vivalto Lungo is a balanced coffee made from a complex blend of separately roasted South American and East African Arabicas, combining roasted and subtle floral notes.",
				4,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Linizio Lungo",
				"ROUND AND SMOOTH",
				"Mild and well-rounded on the palate, Linizio Lungo is a blend of fine Arabica enhancing malt and cereal notes.",
				4,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Decaffeinato Intenso",
				"DENSE AND POWERFUL",
				"Dark roasted South American Arabicas with a touch of Robusta bring out the subtle cocoa and roasted cereal notes of this full-bodied decaffeinated espresso.",
				7,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Decaffeinato Lungo",
				"LIGHT AND FULL-FLAVOURED",
				"The slow roasting of this blend of South American Arabicas with a touch of Robusta gives Decaffeinato Lungo a smooth, creamy body and roasted cereal flavour.",
				3,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Decaffeinato",
				"FRUITY AND DELICATE",
				"A blend of South American Arabicas reinforced with just a touch of Robusta is lightly roasted to reveal an aroma of red fruit.",
				2,
				brandPony,
				makerStable
		);
		createCoffee(
				"Caramelito",
				"CARAMEL FLAVOURED",
				"The sweet flavour of caramel softens the roasted notes of the Livanto Grand Cru. This delicate gourmet marriage evokes the creaminess of soft toffee.",
				6,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Ciocattino",
				"CHOCOLATE FLAVOURED",
				"Dark and bitter chocolate notes meet the caramelized roast of the Livanto Grand Cru. A rich combination reminiscent of a square of dark chocolate.",
				6,
				brandMonkey,
				makerZoo
		);
		createCoffee(
				"Vanilio",
				"VANILLA FLAVOURED",
				"A balanced harmony between the rich and the velvety aromas of vanilla and the mellow flavour of the Livanto Grand Cru. A blend distinguished by its full flavour, infinitely smooth and silky on the palate.",
				6,
				brandMonkey,
				makerZoo
		);

		helper.add(
				new Book( "Le chat qui regardait les étoiles", "Lilian Jackson Braun" ),
				new Book( "Le chat qui déplaçait des montagnes", "Lilian Jackson Braun" ),
				new Book( "Le Grand Molière illustré", "Caroline Guillot" ),
				new Book( "Tartuffe", "Molière" ),
				new Book( "Dom Garcie de Navarre", "moliere" ) // Molière all lowercase and without an accent
		);
	}

	private void createCoffee(String name, String summary, String description, int intensity, CoffeeBrand brand,
			CoffeeMaker maker) {
		Coffee coffee = new Coffee();
		coffee.setId( name );
		coffee.setName( name );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		coffee.setBrand( brand );
		coffee.setMaker( maker );
		helper.add( coffee );
	}
}
