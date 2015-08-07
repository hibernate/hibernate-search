/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
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
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( SomeEntity.class )
			.withProperty( org.hibernate.search.cfg.Environment.DEFAULT_NULL_TOKEN, "-7" );

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

		List<EntityInfo> queryEntityInfos = runProjection( query, "title" );

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Assert.assertEquals( "title-two", entityInfo.getProjection()[0] );
	}

	@Test
	public void verifyExplicitKeywordQuery() {
		Query query = getQueryBuilder()
					.keyword()
					.onField( "age" )
					.matching( 2 )
					.createQuery();

		Assert.assertTrue( query instanceof NumericRangeQuery );

		List<EntityInfo> queryEntityInfos = runProjection( query, "title" );

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Assert.assertEquals( "title-two", entityInfo.getProjection()[0] );
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

		List<EntityInfo> queryEntityInfos = runProjection( query, "title" );

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Assert.assertEquals( "title-two", entityInfo.getProjection()[0] );
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

		List<EntityInfo> queryEntityInfos = runProjection( query, "title" );

		Assert.assertEquals( 0, queryEntityInfos.size() );
	}

	private List<EntityInfo> runProjection(Query query, String fieldName) {
		return sfHolder
				.getSearchFactory()
				.createHSQuery().luceneQuery( query )
				.targetedEntities( Arrays.asList( new Class<?>[] { SomeEntity.class } ) )
				.projection( fieldName )
				.queryEntityInfos();
	}

	private QueryBuilder getQueryBuilder() {
		return sfHolder
				.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( SomeEntity.class )
				.get();
	}

	private void storeData(String title, int value, Integer nullableAge) {
		SomeEntity entry = new SomeEntity();
		entry.title = title;
		entry.age = value;
		entry.nullableAge = nullableAge;

		Work work = new Work( entry, entry.title, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		sfHolder.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
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
