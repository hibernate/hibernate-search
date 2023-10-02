/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.SoftAssertions;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingCachingIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private Statistics statistics;
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );

		sessionFactory = ormSetupHelper.start().withPropertyRadical(
				HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, "false" )
				.withProperty( AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ALL.name() )
				.withProperty( AvailableSettings.GENERATE_STATISTICS, "true" )
				.withProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
	}

	@BeforeEach
	void initData() {
		// This will also add entities to the 2nd level cache
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new IndexedEntity( 1, "text1" ) );
			session.persist( new IndexedEntity( 2, "text2" ) );
			session.persist( new IndexedEntity( 3, "text3" ) );
		} );

		sessionFactory.getCache().evictEntityData( IndexedEntity.class, 1 );

		statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
	}

	@Test
	// Note this test used to pass even before fixed HSEARCH-4272, but only because of another bug: HSEARCH-4273
	@TestForIssue(jiraKey = "HSEARCH-4272")
	void default_ignore() {
		with( sessionFactory ).runNoTransaction( session -> {
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
			assertSecondLevelCachePutCount( softly ).isEqualTo( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4273")
	void explicit_get() {
		with( sessionFactory ).runNoTransaction( session -> {
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
			assertSecondLevelCachePutCount( softly ).isEqualTo( 0 );
		} );
	}

	@Test
	void explicit_normal() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer()
					.cacheMode( CacheMode.NORMAL );

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
			assertSecondLevelCachePutCount( softly ).isEqualTo( 1 );
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

	private AbstractLongAssert<?> assertSecondLevelCachePutCount(SoftAssertions softly) {
		return softly.assertThat( statistics.getSecondLevelCachePutCount() )
				.as( "Second level cache put count" );
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
