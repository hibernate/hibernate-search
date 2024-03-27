/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils.prepareBooks;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.countAll;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test behavior when the index becomes out of sync with the database.
 */
class OutOfSyncIndexIT {

	private static final int NUMBER_OF_BOOKS = 8;

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void before() {
		entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( Book.class );

		prepareBooks( entityManagerFactory, NUMBER_OF_BOOKS );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void search_skipDeletedEntitiesInHits() {
		// Check that document counts are identical
		int entityCount = entityCount();
		int indexedEntityCount = BookCreatorUtils.documentsCount( entityManagerFactory ).intValue();
		assertThat( entityCount ).isPositive();
		assertThat( indexedEntityCount ).isEqualTo( entityCount );

		// Simulate an external delete that Hibernate Search will not be able to detect
		with( entityManagerFactory ).runInTransaction(
				entityManager -> entityManager.createQuery( "DELETE FROM book WHERE MOD(id, 2) = 0" )
						.executeUpdate()
		);

		// Check that document counts are off
		int entityCountAfterQuery = entityCount();
		indexedEntityCount = BookCreatorUtils.documentsCount( entityManagerFactory ).intValue();
		assertThat( entityCountAfterQuery ).isPositive();
		assertThat( indexedEntityCount ).isGreaterThan( entityCountAfterQuery );

		// Check that running a search query still returns the correct number of hits,
		// because hits that cannot be loaded are ignored
		with( entityManagerFactory ).runInTransaction( entityManager -> assertThat(
				Search.session( entityManager )
						.search( Book.class )
						.where( p -> p.matchAll() )
						.fetchAllHits()
		).hasSize( entityCountAfterQuery )
				.doesNotContainNull()
		);
	}

	private int entityCount() {
		return countAll( entityManagerFactory.createEntityManager(), Book.class ).intValue();
	}

}
