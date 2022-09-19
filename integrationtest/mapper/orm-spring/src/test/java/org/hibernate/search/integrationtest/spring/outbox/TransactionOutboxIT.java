/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles(profiles = { "outbox" })
public class TransactionOutboxIT {

	@Configuration
	@EntityScan
	@ComponentScan(basePackageClasses = TransactionOutboxIT.class)
	public static class SpringConfig extends AbstractSpringITConfig {

		@Override
		public BackendMock backendMock() {
			BackendMock backendMock = super.backendMock();
			backendMock.indexingWorkExpectations( CoordinationStrategyExpectations.outboxPolling().indexingWorkExpectations );
			return backendMock;
		}
	}

	@Autowired
	@Rule
	public BackendMock backendMock;

	@Autowired
	private HelperService helperService;

	@Test
	public void persist() {
		Integer id = 1;

		backendMock.expectWorks( IndexedEntity.NAME )
				.add( id.toString(), b -> {
				} );

		helperService.persist( id );
	}

	@Service
	@Transactional
	public static class HelperService {
		@Autowired
		private EntityManager entityManager;

		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void persist(Integer innerId) {
			IndexedEntity entity = new IndexedEntity( innerId );
			entityManager.persist( entity );
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