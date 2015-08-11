/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.time;

import static org.fest.assertions.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-1947")
public class JavaTimeTest extends SearchTestBase {

	@After
	public void deleteEntity() {
		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.delete( s.load( Sample.class, 1L ) );
			s.flush();
			tx.commit();
		}
	}

	@Test
	public void testLocalDate() throws Exception {
		LocalDate date = LocalDate.of( 2012, Month.DECEMBER, 30 );
		Sample sample = new Sample( 1L, "LocalDate example" );
		sample.localDate = date;

		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.persist( sample );
			s.flush();
			tx.commit();

			tx = s.beginTransaction();
			final FullTextSession session = Search.getFullTextSession( s );
			String field = "localDate";

			Query query = queryBuilder( session ).keyword().onField( field ).ignoreAnalyzer().matching( date ).createQuery();
			Object[] result = (Object[]) session.createFullTextQuery( query ).setProjection( field ).uniqueResult();

			assertThat( result ).containsOnly( date );

			tx.commit();
		}

	}

	@Test
	public void testLocalTime() throws Exception {
		LocalTime time = LocalTime.of( 13, 15, 55, 7 );

		Sample sample = new Sample( 1L, "Local time example" );
		sample.localTime = time;

		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.persist( sample );
			s.flush();
			tx.commit();

			tx = s.beginTransaction();
			final FullTextSession session = Search.getFullTextSession( s );
			String field = "localTime";

			Query query = queryBuilder( session ).keyword().onField( field ).ignoreAnalyzer().matching( time ).createQuery();
			Object[] result = (Object[]) session.createFullTextQuery( query ).setProjection( field ).uniqueResult();
			assertThat( result ).containsOnly( time );

			tx.commit();
		}
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );

		Sample sample = new Sample( 1L, "LocalDateTime example" );
		sample.localDateTime = dateTime;

		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.persist( sample );
			s.flush();
			tx.commit();

			tx = s.beginTransaction();
			final FullTextSession session = Search.getFullTextSession( s );
			String field = "localDateTime";

			Query query = queryBuilder( session ).keyword().onField( field ).ignoreAnalyzer().matching( dateTime ).createQuery();
			Object[] result = (Object[]) session.createFullTextQuery( query ).setProjection( field ).uniqueResult();

			assertThat( result ).containsOnly( dateTime );

			tx.commit();
		}
	}

	@Test
	public void testInstant() throws Exception {
		LocalDate date = LocalDate.of( 1998, Month.FEBRUARY, 12 );
		LocalTime time = LocalTime.of( 13, 05, 33, 5 * 1000_000 );
		LocalDateTime dateTime = LocalDateTime.of( date, time );
		Instant instant = dateTime.toInstant( ZoneOffset.UTC );

		Sample sample = new Sample( 1L, "Instant example" );
		sample.instant = instant;

		try (org.hibernate.Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			s.persist( sample );
			s.flush();
			tx.commit();

			tx = s.beginTransaction();
			final FullTextSession session = Search.getFullTextSession( s );
			String field = "instant";

			Query query = queryBuilder( session ).keyword().onField( field ).ignoreAnalyzer().matching( instant ).createQuery();
			Object[] result = (Object[]) session.createFullTextQuery( query ).setProjection( field ).uniqueResult();

			assertThat( result ).containsOnly( instant );

			tx.commit();
		}
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	private QueryBuilder queryBuilder(final FullTextSession session) {
		QueryBuilder builder = session.getSearchFactory().buildQueryBuilder().forEntity( Sample.class ).get();
		return builder;
	}

	@Entity
	@Indexed
	static class Sample {

		public Sample() {
		}

		public Sample(long id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		@DocumentId
		long id;

		@Field(analyze = Analyze.NO, store = Store.YES)
		String description;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalDate localDate;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalTime localTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private LocalDateTime localDateTime;

		@Field(analyze = Analyze.NO, store = Store.YES)
		private Instant instant;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Sample.class };
	}
}
