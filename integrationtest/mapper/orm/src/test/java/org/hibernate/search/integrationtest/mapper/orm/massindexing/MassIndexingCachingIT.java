/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.stat.Statistics;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.SoftAssertions;

public class MassIndexingCachingIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private Statistics statistics;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.NAME );

		setupContext.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED, "false" )
				.withProperty( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ALL.name() )
				.withProperty( AvailableSettings.GENERATE_STATISTICS, "true" )
				// it seems that without a query cache hit, we cannot have any second level cache hits,
				// even if these caches I think they are supposed to be independent of each other:
				.withProperty( AvailableSettings.USE_QUERY_CACHE, "true" )
				.withProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" )
				.withAnnotatedTypes( IndexedEntity.class );
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			session.persist( new IndexedEntity( 1, "text1" ) );
			session.persist( new IndexedEntity( 2, "text2" ) );
			session.persist( new IndexedEntity( 3, "text3" ) );
		} );
		setupHolder.runInTransaction( session -> {
			// Load entities to populate 2nd lvl caches:
			TypedQuery<IndexedEntity> query = cachedQuery( session, CacheMode.PUT );
			List<IndexedEntity> entities = query.getResultList();

			assertThat( entities ).hasSize( 3 );
		} );

		setupHolder.sessionFactory().getCache().evictEntityData( IndexedEntity.class, 1 );

		statistics = setupHolder.sessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
	}

	@Test
	// Note this test used to pass even before fixed HSEARCH-4272, but only because of another bug: HSEARCH-4273
	@TestForIssue(jiraKey = "HSEARCH-4272")
	public void default_ignore() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			backendMock.expectWorks( IndexedEntity.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "text", "text1" ) )
					.add( "2", b -> b.field( "text", "text2" ) )
					.add( "3", b -> b.field( "text", "text3" ) );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( IndexedEntity.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();

		assertSoftly( softly -> {
			assertEntityLoadCount( softly ).isEqualTo( 3 );
			assertSecondLevelCacheHitCount( softly ).isEqualTo( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4273")
	public void explicit_get() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer()
					.cacheMode( CacheMode.GET );

			backendMock.expectWorks( IndexedEntity.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b.field( "text", "text1" ) )
					.add( "2", b -> b.field( "text", "text2" ) )
					.add( "3", b -> b.field( "text", "text3" ) );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( IndexedEntity.NAME, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();

		assertSoftly( softly -> {
			assertEntityLoadCount( softly ).isEqualTo( 1 );
			assertSecondLevelCacheHitCount( softly ).isEqualTo( 2 );
		} );
	}

	private AbstractLongAssert<?> assertEntityLoadCount(SoftAssertions softly) {
		return softly.assertThat( statistics.getEntityLoadCount() )
				.as( "Entity load count" );
	}

	private AbstractLongAssert<?> assertSecondLevelCacheHitCount(SoftAssertions softly) {
		return softly.assertThat( statistics.getSecondLevelCacheHitCount() )
				.as( "Second level cache hit count" );
	}

	private Query cachedQuery(Session session, CacheMode cacheMode) {
		Query<IndexedEntity> query = session.createQuery(
				"select e from IndexedEntity e where e.id in (:ids)",
				IndexedEntity.class
		);

		query.setParameter( "ids", Arrays.asList( 1, 2, 3 ) )
				.setCacheMode( cacheMode )
				.setHint( QueryHints.HINT_CACHEABLE, true );

		return query;
	}

	@Entity(name = IndexedEntity.NAME)
	@Table
	@Indexed
	public static class IndexedEntity {

		public static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
