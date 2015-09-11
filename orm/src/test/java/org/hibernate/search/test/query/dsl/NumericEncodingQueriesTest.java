/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;
import org.fest.assertions.Assertions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class NumericEncodingQueriesTest extends SearchTestBase {

	private static final Calendar ANNOUNCED = initCalendar( 1950, 1, 1 );
	private static final Date UPDATED = initCalendar( 2000, 1, 1 ).getTime();
	private static final Calendar FIRST_EDITION = initCalendar( 1966, 0, 1 );
	private static final Calendar NEXT_EVENT = initCalendar( 2015, 9, 29 );
	private static final Fair LUCCA_COMICS = new Fair( 1L, "Lucca comics and games", NEXT_EVENT.getTime(), FIRST_EDITION, UPDATED, ANNOUNCED );

	private static Calendar initCalendar(int year, int month, int day) {
		Calendar instance = createCalendar();
		instance.set( 1966, 0, 1 );
		return instance;
	}

	private static Calendar createCalendar() {
		return Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ITALY );
	}

	@Before
	public void createEvent() throws Exception {
		try (Session session = openSession()) {
			Transaction tx = session.beginTransaction();
			session.persist( LUCCA_COMICS );
			tx.commit();
		}
	}

	@After
	public void cleanUp() throws Exception {
		try (Session session = openSession()) {
			Transaction tx = session.beginTransaction();
			session.delete( LUCCA_COMICS );
			tx.commit();
		}
	}

	@Test
	public void testDslWithDate() throws Exception {
		try (Session session = openSession()) {
			Date nextEventDate = DateTools.round( NEXT_EVENT.getTime(), DateTools.Resolution.DAY );
			Query query = queryBuilder().keyword().onField( "startDate" ).matching( nextEventDate ).createQuery();

			Fair event = (Fair) Search.getFullTextSession( session ).createFullTextQuery( query, Fair.class ).uniqueResult();
			Assertions.assertThat( event ).isEqualTo( LUCCA_COMICS );
		}
	}

	@Test
	public void testDslWithCalendar() throws Exception {
		try (Session session = openSession()) {
			Calendar year = createCalendar();
			year.setTime( DateTools.round( FIRST_EDITION.getTime(), DateTools.Resolution.YEAR ) );

			Query query = queryBuilder().keyword().onField( "since" ).matching( year ).createQuery();

			Fair event = (Fair) Search.getFullTextSession( session ).createFullTextQuery( query, Fair.class ).uniqueResult();
			Assertions.assertThat( event ).isEqualTo( LUCCA_COMICS );
		}
	}

	@Test
	public void testDslWithDefaultDateBridge() throws Exception {
		try (Session session = openSession()) {
			Query query = queryBuilder().keyword().onField( "updated" ).matching( UPDATED ).createQuery();

			Fair event = (Fair) Search.getFullTextSession( session ).createFullTextQuery( query, Fair.class ).uniqueResult();
			Assertions.assertThat( event ).isEqualTo( LUCCA_COMICS );
		}
	}

	@Test
	public void testDslWithDefaultCalendarBridge() throws Exception {
		try (Session session = openSession()) {
			Query query = queryBuilder().keyword().onField( "announced" ).matching( ANNOUNCED ).createQuery();

			Fair event = (Fair) Search.getFullTextSession( session ).createFullTextQuery( query, Fair.class ).uniqueResult();
			Assertions.assertThat( event ).isEqualTo( LUCCA_COMICS );
		}
	}

	private QueryBuilder queryBuilder() {
		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Fair.class ).get();
		return queryBuilder;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Fair.class };
	}

	@Entity
	@Indexed
	static class Fair {

		@Id
		private Long id;

		@Field
		private String name;

		@Field
		@DateBridge(encoding = EncodingType.NUMERIC, resolution = Resolution.DAY)
		@Temporal(TemporalType.DATE)
		private Date startDate;

		@Field
		@CalendarBridge(encoding = EncodingType.NUMERIC, resolution = Resolution.YEAR)
		private Calendar since;

		@Field
		private Date updated;

		@Field
		private Calendar announced;

		public Fair() {
		}

		public Fair(Long id, String name, Date startDate, Calendar since, Date updated, Calendar announced) {
			super();
			this.id = id;
			this.name = name;
			this.startDate = startDate;
			this.since = since;
			this.updated = updated;
			this.announced = announced;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getStartDate() {
			return startDate;
		}

		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}

		public Calendar getSince() {
			return since;
		}

		public void setSince(Calendar firstEvent) {
			this.since = firstEvent;
		}

		public Date getUpdated() {
			return updated;
		}

		public void setUpdated(Date updated) {
			this.updated = updated;
		}

		public Calendar getAnnounced() {
			return announced;
		}

		public void setAnnounced(Calendar announced) {
			this.announced = announced;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Fair other = (Fair) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "Fair [id=" );
			builder.append( id );
			builder.append( ", name=" );
			builder.append( name );
			builder.append( ", startDate=" );
			builder.append( startDate );
			builder.append( ", since=" );
			builder.append( since );
			builder.append( "]" );
			return builder.toString();
		}
	}

}
