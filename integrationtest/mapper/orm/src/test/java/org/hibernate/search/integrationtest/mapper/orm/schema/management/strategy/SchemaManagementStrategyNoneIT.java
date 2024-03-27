/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SchemaManagementStrategyNoneIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void none() {
		SessionFactory sessionFactory = setup();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();

		sessionFactory.close();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();
	}

	private SessionFactory setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		return setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						getStrategyName()
				)
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	protected String getStrategyName() {
		return SchemaManagementStrategyName.NONE.externalRepresentation();
	}

	@Entity(name = IndexedEntity1.NAME)
	@Indexed(index = IndexedEntity1.NAME)
	private static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@Id
		private Integer id;
	}

	@Entity(name = IndexedEntity2.NAME)
	@Indexed(index = IndexedEntity2.NAME)
	private static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@Id
		private Integer id;
	}
}
