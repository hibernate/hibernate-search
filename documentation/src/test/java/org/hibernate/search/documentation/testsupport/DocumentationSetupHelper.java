/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

public final class DocumentationSetupHelper
		extends MappingSetupHelper<DocumentationSetupHelper.SetupContext, SimpleSessionFactoryBuilder, SessionFactory> {

	private static final String DEFAULT_BACKEND_NAME = "backendName";

	public static DocumentationSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return withSingleBackend( DEFAULT_BACKEND_NAME, backendConfiguration );
	}

	public static DocumentationSetupHelper withSingleBackend(String backendName, BackendConfiguration backendConfiguration) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendName, backendConfiguration )
		);
	}

	public static DocumentationSetupHelper withMultipleBackends(String defaultBackendName,
			Map<String, BackendConfiguration> backendConfigurations) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendName, backendConfigurations )
		);
	}

	private DocumentationSetupHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext();
	}

	@Override
	protected void close(SessionFactory toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SimpleSessionFactoryBuilder, SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext() {
			// Real backend => ensure we clean up everything before and after the tests
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
					SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP );
			// Override the automatic indexing synchronization strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
					AutomaticIndexingSynchronizationStrategyNames.SYNC );
			// Ensure overridden properties will be applied
			withConfiguration( builder -> overriddenProperties.forEach( builder::setProperty ) );
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