/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractMapperOrmSpringIT;
import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Adding a property here is just a "workaround" to make sure that a different context is used for this test
// otherwise there can be build errors when running all the tests via maven.
@SpringBootTest(properties = "spring.datasource.name=hsearch-datasource1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionIT extends AbstractMapperOrmSpringIT {

	@Configuration
	@EntityScan
	@ComponentScan(basePackageClasses = TransactionIT.class)
	public static class SpringConfig extends AbstractSpringITConfig {
	}

	@Autowired
	@RegisterExtension
	public BackendMock backendMock;

	@Autowired
	private HelperService helperService;

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1270")
	void innerTransactionRollback() {
		Integer outerId = 1;
		Integer innerId = 2;

		// Check that inner transaction data is NOT pushed to the index (processed, but discarded).
		backendMock.expectWorks( IndexedEntity.NAME )
				.createAndDiscardFollowingWorks()
				.add( innerId.toString(), b -> {} );
		// Check that outer transaction data is pushed to the index.
		backendMock.expectWorks( IndexedEntity.NAME )
				.add( outerId.toString(), b -> {} );

		helperService.doOuter( outerId, innerId );

		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1270")
	void innerTransactionRollback_flushBeforeInner() {
		Integer outerId = 1;
		Integer innerId = 2;

		// Check that outer transaction data is processed.
		backendMock.expectWorks( IndexedEntity.NAME )
				.createFollowingWorks()
				.add( outerId.toString(), b -> {} );
		// Check that inner transaction data is NOT pushed to the index (processed, but discarded).
		backendMock.expectWorks( IndexedEntity.NAME )
				.createAndDiscardFollowingWorks()
				.add( innerId.toString(), b -> {} );
		// Check that outer transaction data is pushed to the index.
		backendMock.expectWorks( IndexedEntity.NAME )
				.executeFollowingWorks()
				.add( outerId.toString(), b -> {} );

		helperService.doOuterFlushBeforeInner( outerId, innerId );

		backendMock.verifyExpectationsMet();
	}

	@Service
	@Transactional
	public static class HelperService {

		@Autowired
		@Lazy
		private HelperService self;

		@Autowired
		private EntityManager entityManager;

		/**
		 * Persists a new book within an independent transaction.
		 * Flushes and finally rollback throwing a {@link MakeRollbackException}.
		 * <p>
		 * When the nested transaction rollbacks, this is the case here,
		 * {@code Propagation.REQUIRES_NEW} is equivalent to a {@code Propagation.NESTED},
		 * what does really matter is that:
		 * <b>inner transaction rollback does not affect outer transaction</b>.
		 * <p>
		 * {@code Propagation.REQUIRES_NEW} differs from {@code Propagation.NESTED}
		 * when outer transaction rollbacks, this is NOT the case here,
		 * with {@code Propagation.REQUIRES_NEW} the inner transaction can commit,
		 * with {@code Propagation.NESTED} the inner transaction cannot commit.
		 *
		 * @param innerId the id for the entity that has to be created within the inner transaction
		 * @throws MakeRollbackException
		 */
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void doInner(Integer innerId) {
			// This data is supposed to be NOT pushed to the index:
			IndexedEntity entity = new IndexedEntity( innerId );
			entityManager.persist( entity );
			entityManager.flush();
			throw new MakeRollbackException( "Rollback the inner transaction" );
		}

		public void doOuter(Integer outerId, Integer innerId) {
			// This data is supposed to be pushed to the index:
			IndexedEntity entity = new IndexedEntity( outerId );
			entityManager.persist( entity );
			try {
				self.doInner( innerId );
			}
			catch (MakeRollbackException e) {
				// Do not rollback the outer transaction
			}
			entityManager.flush();
		}

		public void doOuterFlushBeforeInner(Integer outerId, Integer innerId) {
			// This data is supposed to be pushed to the index:
			IndexedEntity entity = new IndexedEntity( outerId );
			entityManager.persist( entity );

			// flush before the execution of the inner transaction
			entityManager.flush();

			try {
				self.doInner( innerId );
			}
			catch (MakeRollbackException e) {
				// Do not rollback the outer transaction
			}
			entityManager.flush();
		}

		private static class MakeRollbackException extends RuntimeException {
			public MakeRollbackException(String message) {
				super( message );
			}
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		public static final String NAME = "Indexed";

		@Id
		private Integer id;

		IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
