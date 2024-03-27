/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl.embedded;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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
 * @author Davide D'Alto
 */
class DslEmbeddedSearchTest {

	private static Calendar initCalendar(int year, int month, int day) {
		Calendar instance = createCalendar();
		instance.clear();
		instance.set( year, month, day );
		return instance;
	}

	private static Calendar createCalendar() {
		return Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ITALY );
	}

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( ContainerEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void setUp() {
		EmbeddedEntity ee = new EmbeddedEntity();
		ee.setEmbeddedField( "embedded" );
		ee.setDate( initCalendar( 2007, Calendar.JANUARY, 14 ).getTime() );

		ContainerEntity pe = new ContainerEntity();
		pe.setId( 1L );
		pe.setEmbeddedEntity( ee );
		pe.setParentStringValue( "theparentvalue" );

		helper.add( pe );

		EmbeddedEntity ee2 = new EmbeddedEntity();
		ee2.setEmbeddedField( "otherembedded" );
		ee2.setDate( initCalendar( 2007, Calendar.JANUARY, 12 ).getTime() );

		ContainerEntity pe2 = new ContainerEntity();
		pe2.setId( 2L );
		pe2.setEmbeddedEntity( ee2 );
		pe2.setParentStringValue( "theotherparentvalue" );

		helper.add( pe2 );
	}

	@Test
	void testSearchString() {
		QueryBuilder qb = helper.queryBuilder( ContainerEntity.class );
		Query q = qb.keyword().onField( "emb.embeddedField" ).matching( "embedded" ).createQuery();

		assertQuery( q ).matchesExactlyIds( 1L );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2070")
	void testSearchDateWithoutFieldBridge() {
		QueryBuilder qb = helper.queryBuilder( ContainerEntity.class );
		Query q = qb.range().onField( "emb.date" )
				.above( initCalendar( 2007, Calendar.JANUARY, 14 ).getTime() )
				.createQuery();

		assertQuery( q ).matchesExactlyIds( 1L );
	}

	private AssertBuildingHSQueryContext assertQuery(Query q) {
		return helper.assertThatQuery( q ).from( ContainerEntity.class );
	}
}
