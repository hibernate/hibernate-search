/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionIT {

	@Configuration
	@EntityScan
	@ComponentScan(basePackageClasses = TransactionIT.class)
	public static class SpringConfig extends AbstractSpringITConfig {
	}

	@Autowired
	@Rule
	public BackendMock backendMock;

	@Autowired
	private HelperService helperService;

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1270" )
	public void innerTransactionRollback() {
		Integer outerId = 1;
		Integer innerId = 2;

		// Check that inner transaction data is NOT pushed to the index (processed, but discarded).
		backendMock.expectWorks( IndexedEntity.NAME )
				.createAndDiscardFollowingWorks()
				.add( innerId.toString(), b -> { } );
		// Check that outer transaction data is pushed to the index.
		backendMock.expectWorks( IndexedEntity.NAME )
				.add( outerId.toString(), b -> { } );

		helperService.doOuter( outerId, innerId );

		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1270" )
	public void innerTransactionRollback_flushBeforeInner() {
		Integer outerId = 1;
		Integer innerId = 2;

		// Check that outer transaction data is processed.
		backendMock.expectWorks( IndexedEntity.NAME )
				.createFollowingWorks()
				.add( outerId.toString(), b -> { } );
		// Check that inner transaction data is NOT pushed to the index (processed, but discarded).
		backendMock.expectWorks( IndexedEntity.NAME )
				.createAndDiscardFollowingWorks()
				.add( innerId.toString(), b -> { } );
		// Check that outer transaction data is pushed to the index.
		backendMock.expectWorks( IndexedEntity.NAME )
				.executeFollowingWorks()
				.add( outerId.toString(), b -> { } );

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