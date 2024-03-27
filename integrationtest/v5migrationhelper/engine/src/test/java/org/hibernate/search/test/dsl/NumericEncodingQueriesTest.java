/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 */
class NumericEncodingQueriesTest {

	private static final Calendar ANNOUNCED = initCalendar( 1950, 1, 1 );
	private static final Date UPDATED = initCalendar( 2000, 1, 1 ).getTime();
	private static final Calendar FIRST_EDITION = initCalendar( 1966, 0, 1 );
	private static final Calendar NEXT_EVENT = initCalendar( 2015, 9, 29 );
	private static final long LUCCA_ID = 1L;

	private static Calendar initCalendar(int year, int month, int day) {
		Calendar instance = createCalendar();
		instance.set( 1966, 0, 1 );
		return instance;
	}

	private static Calendar createCalendar() {
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ITALY );
		calendar.setTimeInMillis( 0 ); // Reset to epoch; clears hours/minutes/seconds in particular
		return calendar;
	}

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Fair.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void createEvent() {
		Fair lucca = new Fair( LUCCA_ID, "Lucca comics and games", NEXT_EVENT.getTime(), FIRST_EDITION, UPDATED, ANNOUNCED );
		helper.add( lucca );
	}

	@Test
	void testDslWithDate() {
		Date nextEventDate = DateTools.round( NEXT_EVENT.getTime(), DateTools.Resolution.DAY );
		Query query = queryBuilder().keyword().onField( "startDate" ).matching( nextEventDate ).createQuery();

		helper.assertThatQuery( query ).from( Fair.class ).matchesExactlyIds( LUCCA_ID );
	}

	@Test
	void testDslWithCalendar() {
		Calendar year = createCalendar();
		year.setTime( DateTools.round( FIRST_EDITION.getTime(), DateTools.Resolution.YEAR ) );

		Query query = queryBuilder().keyword().onField( "since" ).matching( year ).createQuery();

		helper.assertThatQuery( query ).from( Fair.class ).matchesExactlyIds( LUCCA_ID );
	}

	@Test
	void testDslWithDefaultDateBridge() {
		Query query = queryBuilder().keyword().onField( "updated" ).matching( UPDATED ).createQuery();

		helper.assertThatQuery( query ).from( Fair.class ).matchesExactlyIds( LUCCA_ID );
	}

	@Test
	void testDslWithDefaultCalendarBridge() {
		Query query = queryBuilder().keyword().onField( "announced" ).matching( ANNOUNCED ).createQuery();

		helper.assertThatQuery( query ).from( Fair.class ).matchesExactlyIds( LUCCA_ID );
	}

	private QueryBuilder queryBuilder() {
		return helper.queryBuilder( Fair.class );
	}

	@Indexed
	private static class Fair {

		@DocumentId
		private Long id;

		@Field
		private String name;

		@Field
		@DateBridge(resolution = Resolution.DAY)
		private Date startDate;

		@Field
		@CalendarBridge(resolution = Resolution.YEAR)
		private Calendar since;

		@Field
		private Date updated;

		@Field
		private Calendar announced;

		public Fair(Long id, String name, Date startDate, Calendar since, Date updated, Calendar announced) {
			super();
			this.id = id;
			this.name = name;
			this.startDate = startDate;
			this.since = since;
			this.updated = updated;
			this.announced = announced;
		}
	}

}
