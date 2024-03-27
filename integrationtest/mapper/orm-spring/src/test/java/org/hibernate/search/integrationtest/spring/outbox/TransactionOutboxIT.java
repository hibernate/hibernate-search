/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractMapperOrmSpringIT;
import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles(profiles = { "outbox" })
class TransactionOutboxIT extends AbstractMapperOrmSpringIT {

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
	@RegisterExtension
	public BackendMock backendMock;

	@Autowired
	private HelperService helperService;

	@Test
	void persist() {
		Integer id = 1;

		backendMock.expectWorks( IndexedEntity.NAME )
				.add( id.toString(), b -> {} );

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
