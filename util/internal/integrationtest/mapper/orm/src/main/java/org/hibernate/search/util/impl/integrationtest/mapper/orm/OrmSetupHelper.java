/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public final class OrmSetupHelper
		extends MappingSetupHelper<OrmSetupHelper.SetupContext, SimpleSessionFactoryBuilder, SessionFactory> {

	private static final String DEFAULT_BACKEND_NAME = "backendName";

	public static OrmSetupHelper withBackendMock(BackendMock backendMock) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withBackendMocks( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock, BackendMock ... otherBackendMocks) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withBackendMocks( defaultBackendMock, otherBackendMocks ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return withSingleBackend( DEFAULT_BACKEND_NAME, backendConfiguration );
	}

	public static OrmSetupHelper withSingleBackend(String backendName, BackendConfiguration backendConfiguration) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendName, backendConfiguration ),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	public static OrmSetupHelper withMultipleBackends(String defaultBackendName,
			Map<String, BackendConfiguration> backendConfigurations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendName, backendConfigurations ),
				// Real backend => ensure to clean up everything
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	private final SchemaManagementStrategyName schemaManagementStrategyName;

	private OrmSetupHelper(BackendSetupStrategy backendSetupStrategy,
			SchemaManagementStrategyName schemaManagementStrategyName) {
		super( backendSetupStrategy );
		this.schemaManagementStrategyName = schemaManagementStrategyName;
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext( schemaManagementStrategyName );
	}

	@Override
	protected void close(SessionFactory toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SimpleSessionFactoryBuilder, SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext(SchemaManagementStrategyName schemaManagementStrategyName) {
			// Override the schema management strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, schemaManagementStrategyName );
			// Ensure overridden properties will be applied
			withConfiguration( builder -> overriddenProperties.forEach( builder::setProperty ) );
		}

		public SetupContext withTcclLookupPrecedenceBefore() {
			withConfiguration( builder -> builder.setTcclLookupPrecedenceBefore() );
			return thisAsC();
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			overriddenProperties.put( key, value );
			return thisAsC();
		}

		public SessionFactory setup(Class<?> ... annotatedTypes) {
			return withConfiguration( builder -> builder.addAnnotatedClasses( Arrays.asList( annotatedTypes ) ) )
					.setup();
		}

		@Override
		protected SimpleSessionFactoryBuilder createBuilder() {
			return new SimpleSessionFactoryBuilder();
		}

		@Override
		protected SessionFactory build(SimpleSessionFactoryBuilder builder) {
			return builder.build();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}

}
