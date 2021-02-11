/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl.MultitenancyTestHelper;

public final class OrmSetupHelper
		extends MappingSetupHelper<OrmSetupHelper.SetupContext, SimpleSessionFactoryBuilder, SessionFactory> {

	public static void automaticIndexingStrategyExpectations(
			AutomaticIndexingStrategyExpectations automaticIndexingStrategyExpectations) {
		OrmSetupHelper.automaticIndexingStrategyExpectations = automaticIndexingStrategyExpectations;
	}

	private static AutomaticIndexingStrategyExpectations automaticIndexingStrategyExpectations =
			AutomaticIndexingStrategyExpectations.defaults();

	public static OrmSetupHelper withBackendMock(BackendMock backendMock) {
		backendMock.indexingWorkThreadingExpectations( automaticIndexingStrategyExpectations.indexingWorkThreadingExpectations );
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackendMock( backendMock ),
				Collections.singleton( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withBackendMocks(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks) {
		List<BackendMock> backendMocks = new ArrayList<>();
		backendMocks.add( defaultBackendMock );
		backendMocks.addAll( namedBackendMocks.values() );
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackendMocks( defaultBackendMock, namedBackendMocks ),
				backendMocks,
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static OrmSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				Collections.emptyList(),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	public static OrmSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return new OrmSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				Collections.emptyList(),
				// Real backend => ensure to clean up everything
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	private final Collection<BackendMock> backendMocks;
	private final SchemaManagementStrategyName schemaManagementStrategyName;

	private OrmSetupHelper(BackendSetupStrategy backendSetupStrategy, Collection<BackendMock> backendMocks,
			SchemaManagementStrategyName schemaManagementStrategyName) {
		super( backendSetupStrategy );
		this.backendMocks = backendMocks;
		this.schemaManagementStrategyName = schemaManagementStrategyName;
	}

	public boolean areEntitiesProcessedInSession() {
		return automaticIndexingStrategyExpectations.sync;
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext( schemaManagementStrategyName );
	}

	@Override
	protected void init() {
		for ( BackendMock backendMock : backendMocks ) {
			backendMock.indexingWorkThreadingExpectations(
					automaticIndexingStrategyExpectations.indexingWorkThreadingExpectations );
		}
	}

	@Override
	protected void close(SessionFactory toClose) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SessionFactory::close, toClose );
		}
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SimpleSessionFactoryBuilder, SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext(SchemaManagementStrategyName schemaManagementStrategyName) {
			// Override the schema management strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, schemaManagementStrategyName );
			// Set the automatic indexing strategy according to the expectations
			withProperty( "hibernate.search.automatic_indexing.strategy",
					automaticIndexingStrategyExpectations.strategyClassName );
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

		public SetupContext tenants(String ... tenants) {
			withConfiguration( b -> MultitenancyTestHelper.enable( b, tenants ) );
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
