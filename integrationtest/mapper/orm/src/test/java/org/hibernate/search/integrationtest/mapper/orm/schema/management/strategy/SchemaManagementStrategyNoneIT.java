/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class SchemaManagementStrategyNoneIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void none() {
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
