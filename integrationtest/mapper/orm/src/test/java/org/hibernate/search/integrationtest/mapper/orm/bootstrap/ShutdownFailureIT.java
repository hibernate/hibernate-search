/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

/**
 * Check that a failing boot correctly propagates exceptions,
 * despite the complex asynchronous code used during boot.
 */
class ShutdownFailureIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@RegisterExtension
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void logException() {
		backendMock.expectAnySchema( FailingIndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start().setup( FailingIndexedEntity.class );

		logged.expectEvent( Level.ERROR, "Simulated shutdown failure" );
		sessionFactory.close();
	}

	@Entity(name = FailingIndexedEntity.NAME)
	@Indexed(index = FailingIndexedEntity.NAME)
	private static class FailingIndexedEntity {
		static final String NAME = "failingIndexed";

		@Id
		private Integer id;

		// This should trigger a failure at shutdown
		@GenericField(valueBridge = @ValueBridgeRef(type = FailingCloseBridge.class))
		private String field;
	}

	public static class FailingCloseBridge implements ValueBridge<String, String> {
		public FailingCloseBridge() {
		}

		@Override
		public String toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
			throw new IllegalStateException( "Should not be called" );
		}

		@Override
		public void close() {
			throw new RuntimeException( "Simulated shutdown failure" );
		}
	}
}
