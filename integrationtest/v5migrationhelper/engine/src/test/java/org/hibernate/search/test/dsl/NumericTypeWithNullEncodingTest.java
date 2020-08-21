/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A query targeting a numeric-encoded property needs to be a NumericQuery even if it's using 'indexNullAs' markers.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1973")
public class NumericTypeWithNullEncodingTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( SomeEntity.class )
			.withProperty( org.hibernate.search.cfg.Environment.DEFAULT_NULL_TOKEN, "-7" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void prepareTestData() {
		storeData( "title-one", 1, 1 );
		storeData( "title-two", 2, null );
		storeData( "title-three", 3, 3 );
	}

	@Test
	public void verifyExplicitRangeQuery() {
		Query query = getQueryBuilder()
					.range()
						.onField( "age" )
						.from( 1 ).excludeLimit()
						.to( 3 ).excludeLimit()
						.createQuery();

		Assert.assertTrue( query instanceof NumericRangeQuery );

		assertProjection( query, "title" ).matchesExactlySingleProjections( "title-two" );
	}

	@Test
	public void verifyExplicitKeywordQuery() {
		Query query = getQueryBuilder()
					.keyword()
					.onField( "age" )
					.matching( 2 )
					.createQuery();

		Assert.assertTrue( query instanceof NumericRangeQuery );

		assertProjection( query, "title" ).matchesExactlySingleProjections( "title-two" );
	}

	@Test
	public void verifyCustomNullEncoding() {
		Query query = getQueryBuilder()
					.keyword()
					.onField( "nullableAge" )
					.matching( null )
					.createQuery();

		Assert.assertTrue( query instanceof NumericRangeQuery );
		Assert.assertEquals( "[-1 TO -1]", query.toString( "nullableAge" ) );

		assertProjection( query, "title" ).matchesExactlySingleProjections( "title-two" );
	}

	@Test
	public void verifyNullEncoding() {
		Query query = getQueryBuilder()
					.keyword()
					.onField( "age" )
					.matching( null )
					.createQuery();

		Assert.assertTrue( query instanceof NumericRangeQuery );
		Assert.assertEquals( "[-7 TO -7]", query.toString( "age" ) );

		assertProjection( query, "title" ).matchesNone();
	}

	private AssertBuildingHSQueryContext assertProjection(Query query, String fieldName) {
		return helper.assertThat( query )
				.from( SomeEntity.class )
				.projecting( fieldName );
	}

	private QueryBuilder getQueryBuilder() {
		return helper.queryBuilder( SomeEntity.class );
	}

	private void storeData(String title, int value, Integer nullableAge) {
		SomeEntity entry = new SomeEntity();
		entry.title = title;
		entry.age = value;
		entry.nullableAge = nullableAge;

		helper.add( entry );
	}

	@Indexed
	public static class SomeEntity {
		@DocumentId
		String title;

		@Field(indexNullAs = Field.DEFAULT_NULL_TOKEN)
		int age;

		@Field(indexNullAs = "-1")
		Integer nullableAge;
	}

}
