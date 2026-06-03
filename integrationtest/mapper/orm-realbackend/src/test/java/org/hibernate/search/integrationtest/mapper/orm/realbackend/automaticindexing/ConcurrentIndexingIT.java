/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Stress test for concurrent automatic indexing from multiple threads,
 * each performing persist/update/delete cycles.
 * <p>
 * Translated from the removed v5 migration helper {@code WorkerTestCase}.
 */
class ConcurrentIndexingIT {

	private static final int NUMBER_OF_THREADS = 15;
	private static final int ITERATIONS_PER_THREAD = 50;

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	void concurrentPersistUpdateDelete() throws Exception {
		SessionFactory sessionFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.SYNC )
				.skipTestForDialect( SQLServerDialect.class,
						"Concurrent modifications could provoke deadlocks on SQLServer." )
				.skipTestForDialect( CockroachDialect.class,
						"Concurrent modifications could provoke transaction conflicts on CockroachDB." )
				.setup( IndexedPerson.class );

		ExecutorService executor = Executors.newFixedThreadPool( NUMBER_OF_THREADS );
		try {
			List<Future<?>> futures = new ArrayList<>();
			for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
				Runnable task = ( i % 2 == 0 )
						? new PersistUpdateSearchDeleteWork( sessionFactory )
						: new PersistUpdateDeleteWork( sessionFactory );
				for ( int j = 0; j < ITERATIONS_PER_THREAD; j++ ) {
					futures.add( executor.submit( task ) );
				}
			}

			List<Throwable> errors = new ArrayList<>();
			for ( Future<?> future : futures ) {
				try {
					future.get();
				}
				catch (ExecutionException e) {
					errors.add( e.getCause() );
				}
			}
			assertThat( errors )
					.as( "Errors during concurrent indexing" )
					.isEmpty();
		}
		finally {
			executor.shutdown();
			assertThat( executor.awaitTermination( 3, TimeUnit.MINUTES ) )
					.as( "Executor terminated within timeout" )
					.isTrue();
		}
	}

	/**
	 * Persist two entities, update each individually, verify search results, then delete both.
	 */
	private static final class PersistUpdateSearchDeleteWork implements Runnable {
		private final SessionFactory sf;

		PersistUpdateSearchDeleteWork(SessionFactory sf) {
			this.sf = sf;
		}

		@Override
		public void run() {
			int[] ids = with( sf ).applyInTransaction( session -> {
				IndexedPerson p1 = new IndexedPerson();
				p1.setName( "Alice" );
				session.persist( p1 );
				IndexedPerson p2 = new IndexedPerson();
				p2.setName( "Bob" );
				session.persist( p2 );
				return new int[] { p1.getId(), p2.getId() };
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p1 = session.find( IndexedPerson.class, ids[0] );
				p1.setName( "Updated Alice" );
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p2 = session.find( IndexedPerson.class, ids[1] );
				p2.setName( "Updated Bob" );
			} );

			with( sf ).runInTransaction( session -> {
				long count = Search.session( session ).search( IndexedPerson.class )
						.where( f -> f.match().field( "name" ).matching( "Updated Alice" ) )
						.fetchTotalHitCount();
				assertThat( count ).isPositive();
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p1 = session.find( IndexedPerson.class, ids[0] );
				session.remove( p1 );
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p2 = session.find( IndexedPerson.class, ids[1] );
				session.remove( p2 );
			} );
		}
	}

	/**
	 * Persist two entities, update both in a single transaction, then delete both.
	 */
	private static final class PersistUpdateDeleteWork implements Runnable {
		private final SessionFactory sf;

		PersistUpdateDeleteWork(SessionFactory sf) {
			this.sf = sf;
		}

		@Override
		public void run() {
			int[] ids = with( sf ).applyInTransaction( session -> {
				IndexedPerson p1 = new IndexedPerson();
				p1.setName( "Charlie" );
				session.persist( p1 );
				IndexedPerson p2 = new IndexedPerson();
				p2.setName( "Diana" );
				session.persist( p2 );
				return new int[] { p1.getId(), p2.getId() };
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p1 = session.find( IndexedPerson.class, ids[0] );
				p1.setName( "Updated Charlie" );
				IndexedPerson p2 = session.find( IndexedPerson.class, ids[1] );
				p2.setName( "Updated Diana" );
			} );

			with( sf ).runInTransaction( session -> {
				IndexedPerson p1 = session.find( IndexedPerson.class, ids[0] );
				session.remove( p1 );
				IndexedPerson p2 = session.find( IndexedPerson.class, ids[1] );
				session.remove( p2 );
			} );
		}
	}

	@Entity(name = IndexedPerson.NAME)
	@Indexed
	public static class IndexedPerson {
		static final String NAME = "IndexedPerson";

		@Id
		@GeneratedValue
		private Integer id;

		@FullTextField
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
