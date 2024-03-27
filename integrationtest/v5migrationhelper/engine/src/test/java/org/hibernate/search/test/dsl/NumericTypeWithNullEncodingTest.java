/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Query;

/**
 * A query targeting a numeric-encoded property needs to be a NumericQuery even if it's using 'indexNullAs' markers.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1973")
class NumericTypeWithNullEncodingTest {

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( SomeEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void prepareTestData() {
		storeData( "title-one", 1 );
		storeData( "title-two", null );
		storeData( "title-three", 3 );
	}

	@Test
	void verifyExplicitRangeQuery() {
		Query query = getQueryBuilder()
				.range()
				.onField( "nullableAge" )
				.from( 1 ).excludeLimit()
				.to( 3 ).excludeLimit()
				.createQuery();

		assertProjection( query, "title" ).matchesExactlySingleProjections( "title-two" );
	}

	@Test
	void verifyExplicitKeywordQuery() {
		Query query = getQueryBuilder()
				.keyword()
				.onField( "nullableAge" )
				.matching( 2 )
				.createQuery();

		assertProjection( query, "title" ).matchesExactlySingleProjections( "title-two" );
	}

	private AssertBuildingHSQueryContext assertProjection(Query query, String fieldName) {
		return helper.assertThatQuery( query )
				.from( SomeEntity.class )
				.projecting( fieldName );
	}

	private QueryBuilder getQueryBuilder() {
		return helper.queryBuilder( SomeEntity.class );
	}

	private void storeData(String title, Integer nullableAge) {
		SomeEntity entry = new SomeEntity();
		entry.title = title;
		entry.nullableAge = nullableAge;

		helper.add( entry );
	}

	@Indexed
	public static class SomeEntity {
		@DocumentId
		@Field(store = Store.YES)
		String title;

		@Field(indexNullAs = "2")
		Integer nullableAge;
	}

}
