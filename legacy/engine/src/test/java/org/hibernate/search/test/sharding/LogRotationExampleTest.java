/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sharding;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertHSQueryContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * The example scenario: a system which indexes log messages of some periodic event
 * which is recorded every second.
 *
 * By design only one message is expected to be stored for each second.
 * The idea is to shard on a hourly base, and the logs are set to rotate so that the
 * ones older than 24 hours are deleted.
 *
 * This use case benefits from an advanced {@link ShardIdentifierProvider} so that,
 * during a given hour, all writes happen on a specific shard "latest" and all deletes
 * happen on another specific shard "oldest".
 * This approach provides several benefits:
 * - FulltextFiler instances on each of the 22 immutable indexes are fully cacheable
 * - IndexReader instances on these same 22 indexes are never requiring a refresh
 * - Time-Range queries can easily target the subset of indexes they need
 * - Add and Delete operations happen on separate backends, which provides several other
 *  performance benefits, for example an NRT backend can keep writing without needing
 *  to flush (this need is normally triggered by a delete).
 *
 * This test is intentionally not using the Hibernate ORM API as it is likely more
 * suited as a use case for the JBoss Data Grid.
 *
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 * @since 4.4
 */
@TestForIssue(jiraKey = "HSEARCH-1429")
public class LogRotationExampleTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( LogMessage.class )
		.withProperty( "hibernate.search.logs.sharding_strategy", LogMessageShardingStrategy.class.getName() );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void filtersTest() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder() );

		storeLog( makeTimestamp( 2013, 10, 7, 21, 33 ), "implementing method makeTimestamp" );
		storeLog( makeTimestamp( 2013, 10, 7, 21, 35 ), "implementing method storeLog" );
		storeLog( makeTimestamp( 2013, 10, 7, 15, 15 ), "Infinispan team meeting" );
		storeLog( makeTimestamp( 2013, 10, 7, 7, 30 ), "reading another bit from Mordechai Ben-Ari" );
		storeLog( makeTimestamp( 2013, 10, 7, 9, 00 ), "email nightmare begins" );
		storeLog( makeTimestamp( 2013, 10, 7, 9, 50 ), "sync-up with Davide" );
		storeLog( makeTimestamp( 2013, 10, 7, 10, 0 ), "first cofee. At Costa!" );
		storeLog( makeTimestamp( 2013, 10, 7, 10, 10 ), "sync-up with Gunnar and Hardy" );
		storeLog( makeTimestamp( 2013, 10, 7, 10, 20 ), "Checking JIRA state for Hibernate Search release plans" );
		storeLog( makeTimestamp( 2013, 10, 7, 10, 30 ), "Check my Infinispan pull requests from the weekend, cleanup git branches" );
		storeLog( makeTimestamp( 2013, 10, 7, 22, 00 ), "Implementing LogMessageShardingStrategy" );

		QueryBuilder logsQueryBuilder = helper.queryBuilder( LogMessage.class );

		Query allLogs = logsQueryBuilder.all().createQuery();

		assertQuery( allLogs, 0, 24 ).hasResultSize( 11 );
		assertQuery( allLogs, 2, 5 ).hasResultSize( 0 );
		assertQuery( allLogs, 2, 8 ).hasResultSize( 1 );
		assertQuery( allLogs, 0, 10 ).hasResultSize( 3 );

		deleteLog( makeTimestamp( 2013, 10, 7, 9, 00 ) );
		assertQuery( allLogs, 0, 24 ).hasResultSize( 10 );

		Assert.assertEquals( 24, searchFactory.getIndexManagerHolder().getIndexManagers().size() );
	}

	private AssertHSQueryContext assertQuery(Query luceneQuery, int fromHour, int toHour) {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		HSQuery hsQuery = searchFactory.createHSQuery( luceneQuery, LogMessage.class );
		hsQuery
			.enableFullTextFilter( "timeRange" )
				.setParameter( "from", Integer.valueOf( fromHour ) )
				.setParameter( "to", Integer.valueOf( toHour ) )
			;

		return helper.assertThat( hsQuery );
	}

	private void storeLog(long timestamp, String message) {
		LogMessage log = new LogMessage();
		log.timestamp = timestamp;
		log.message = message;
		helper.add( log );
	}

	private void deleteLog(long timestamp) {
		LogMessage log = new LogMessage();
		log.timestamp = timestamp;
		helper.delete( LogMessage.class, log.timestamp );
	}

	/**
	 * A ShardIdentifierProvider suitable for the rotating - logs design
	 * as described in this test.
	 * Sharding isn't actually dynamic as we know all hours in advance, but
	 * both addition and deletion can target a specific index, and a range
	 * filter can make queries need to search only a subset of all indexes.
	 */
	public static final class LogMessageShardingStrategy implements ShardIdentifierProvider {

		private Set<String> hoursOfDay;

		@Override
		public void initialize(Properties properties, BuildContext buildContext) {
			Set<String> hours = new HashSet<String>( 24 );
			for ( int hour = 0; hour < 24; hour++ ) {
				hours.add( String.valueOf( hour ) );
			}
			hoursOfDay = Collections.unmodifiableSet( hours );
		}

		@Override
		public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
			return fromIdToHour( (Long) id );
		}

		@Override
		public Set<String> getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters) {
			for ( FullTextFilterImplementor ftf : fullTextFilters ) {
				if ( "timeRange".equals( ftf.getName() ) ) {
					Integer from = (Integer) ftf.getParameter( "from" );
					Integer to = (Integer) ftf.getParameter( "to" );
					Set<String> hours = new HashSet<String>();
					for ( int hour = from; hour < to; hour++ ) {
						hours.add( String.valueOf( hour ) );
					}
					return Collections.unmodifiableSet( hours );
				}
			}
			return hoursOfDay;
		}

		@Override
		public Set<String> getShardIdentifiersForDeletion(Class<?> entity, Serializable id, String idInString) {
			return getAllShardIdentifiers();
		}

		@Override
		public Set<String> getAllShardIdentifiers() {
			return hoursOfDay;
		}

	}

	@Indexed( index = "logs" )
	@FullTextFilterDef( name = "timeRange", impl = ShardSensitiveOnlyFilter.class )
	public static final class LogMessage {

		private long timestamp;
		private String message;

		@DocumentId
		public long getId() { return timestamp; }

		public void setId(long id) { this.timestamp = id; }

		@Field
		public String getMessage() { return message; }

		public void setMessage(String message) { this.message = message; }
	}

	/**
	 * @return a timestamp from the calendar-style encoding using GMT as timezone (precision to the minute)
	 */
	private static long makeTimestamp(int year, int month, int date, int hourOfDay, int minute) {
		Calendar gmtCalendar = createGMTCalendar();
		gmtCalendar.set( year, month, date, hourOfDay, minute );
		gmtCalendar.set( Calendar.SECOND, 0 );
		gmtCalendar.set( Calendar.MILLISECOND, 0 );
		return gmtCalendar.getTimeInMillis();
	}

	/**
	 * @return the hour of the day from a timetamp, in string format matching the index shard identifiers format
	 */
	private static String fromIdToHour(long millis) {
		Calendar gmtCalendar = createGMTCalendar();
		gmtCalendar.setTimeInMillis( millis );
		return String.valueOf( gmtCalendar.get( Calendar.HOUR_OF_DAY ) );
	}

	/**
	 * @return a new GMT Calendar
	 */
	private static Calendar createGMTCalendar() {
		return Calendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );
	}

}
