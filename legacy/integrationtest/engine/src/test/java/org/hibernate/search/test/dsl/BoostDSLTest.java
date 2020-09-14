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
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HSEARCH-2983")
public class BoostDSLTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Coffee.class, CoffeeBrand.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void setUp() throws Exception {
		indexTestData();
	}

	@Test
	public void testBoostOnTermQuery() {
		QueryBuilder qb = helper.queryBuilder( Coffee.class );

		Query query = qb.bool()
				.should( qb.keyword().onField( "name" ).boostedTo( 40f ).matching( "Kazaar" ).createQuery() )
				.should( qb.keyword().onField( "summary" ).boostedTo( 1f ).matching( "VELVETY" ).createQuery() )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Kazaar", "Dharkan" );

		query = qb.bool()
				.should( qb.keyword().onField( "name" ).boostedTo( 1f ).matching( "Kazaar" ).createQuery() )
				.should( qb.keyword().onField( "summary" ).boostedTo( 40f ).matching( "VELVETY" ).createQuery() )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Dharkan", "Kazaar" );
	}

	@Test
	public void testBoostOnNumericQuery() {
		QueryBuilder qb = helper.queryBuilder( Coffee.class );

		Query query = qb.bool()
				.should( qb.keyword().onField( "name" ).boostedTo( 40f ).matching( "Kazaar" ).createQuery() )
				.should( qb.keyword().onField( "intensity" ).boostedTo( 1f ).matching( 11 ).createQuery() )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Kazaar", "Dharkan" );

		query = qb.bool()
				.should( qb.keyword().onField( "name" ).boostedTo( 1f ).matching( "Kazaar" ).createQuery() )
				.should( qb.keyword().onField( "intensity" ).boostedTo( 40f ).matching( 11 ).createQuery() )
				.createQuery();

		helper.assertThat( query ).from( Coffee.class )
				.sort( new Sort( SortField.FIELD_SCORE ) )
				.matchesExactlyIds( "Dharkan", "Kazaar" );
	}

	private void indexTestData() {
		createCoffee(
				"Kazaar",
				"EXCEPTIONALLY INTENSE AND SYRUPY",
				"A daring blend of two Robustas from Brazil and Guatemala, specially prepared for Nespresso, and a separately roasted Arabica from South America, Kazaar is a coffee of exceptional intensity. Its powerful bitterness and notes of pepper are balanced by a full and creamy texture.",
				12
		);
		createCoffee(
				"Dharkan",
				"LONG ROASTED AND VELVETY",
				"This blend of Arabicas from Latin America and Asia fully unveils its character thanks to the technique of long roasting at a low temperature. Its powerful personality reveals intense roasted notes together with hints of bitter cocoa powder and toasted cereals that express themselves in a silky and velvety txture.",
				11
		);
		createCoffee(
				"Ristretto",
				"POWERFUL AND CONTRASTING",
				"A blend of South American and East African Arabicas, with a touch of Robusta, roasted separately to create the subtle fruity note of this full-bodied, intense espresso.",
				10
		);
	}

	private void createCoffee(String name, String summary, String description, int intensity) {
		Coffee coffee = new Coffee();
		coffee.setId( name );
		coffee.setName( name );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		helper.add( coffee );
	}
}
