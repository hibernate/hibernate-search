/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

public final class DocumentationSetupHelper
		extends MappingSetupHelper<DocumentationSetupHelper.SetupContext, SimpleSessionFactoryBuilder, SessionFactory> {

	public static List<DocumentationSetupHelper> testParamsForBothAnnotationsAndProgrammatic(
			BackendConfiguration backendConfiguration,
			Consumer<ProgrammaticMappingConfigurationContext> programmaticMappingContributor) {
		HibernateOrmSearchMappingConfigurer mappingConfigurer =
				context -> programmaticMappingContributor.accept( context.programmaticMapping() );
		List<DocumentationSetupHelper> result = new ArrayList<>();
		// Annotation-based mapping
		result.add( withSingleBackend( backendConfiguration, null ) );
		// Programmatic mapping
		result.add( withSingleBackend( backendConfiguration, mappingConfigurer ) );
		return result;
	}

	public static DocumentationSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				backendConfiguration,
				null
		);
	}

	public static DocumentationSetupHelper withSingleBackend(BackendConfiguration backendConfiguration,
			HibernateOrmSearchMappingConfigurer mappingConfigurerOrNull) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				backendConfiguration,
				mappingConfigurerOrNull
		);
	}

	public static DocumentationSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations,
			HibernateOrmSearchMappingConfigurer mappingConfigurerOrNull) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				defaultBackendConfiguration,
				mappingConfigurerOrNull
		);
	}

	private final BackendConfiguration defaultBackendConfiguration;

	private final HibernateOrmSearchMappingConfigurer mappingConfigurerOrNull;

	private DocumentationSetupHelper(BackendSetupStrategy backendSetupStrategy,
			BackendConfiguration defaultBackendConfiguration,
			HibernateOrmSearchMappingConfigurer mappingConfigurerOrNull) {
		super( backendSetupStrategy );
		this.defaultBackendConfiguration = defaultBackendConfiguration;
		this.mappingConfigurerOrNull = mappingConfigurerOrNull;
	}

	@Override
	public String toString() {
		return defaultBackendConfiguration.toString()
				+ (mappingConfigurerOrNull != null ? " - programmatic mapping" : "");
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext( mappingConfigurerOrNull );
	}

	@Override
	protected void close(SessionFactory toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SimpleSessionFactoryBuilder, SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext(HibernateOrmSearchMappingConfigurer mappingConfigurerOrNull) {
			// Real backend => ensure we clean up everything before and after the tests
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
					SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP );
			// Override the automatic indexing synchronization strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
					AutomaticIndexingSynchronizationStrategyNames.SYNC );
			// Set up programmatic mapping if necessary
			if ( mappingConfigurerOrNull != null ) {
				withProperty( HibernateOrmMapperSettings.MAPPING_PROCESS_ANNOTATIONS, false );
				withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, mappingConfigurerOrNull );
			}
			// Ensure we don't build Jandex indexes needlessly:
			// discovery based on Jandex ought to be tested in real projects that don't use this setup helper.
			withProperty( HibernateOrmMapperSettings.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES, false );
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
		protected BackendMappingHandle toBackendMappingHandle(SessionFactory result) {
			return new HibernateOrmMappingHandle( result );
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}

}