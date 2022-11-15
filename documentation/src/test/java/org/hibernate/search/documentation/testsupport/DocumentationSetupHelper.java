/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl.MultitenancyTestHelper;

import org.junit.jupiter.params.provider.Arguments;

public final class DocumentationSetupHelper
		extends
		MappingSetupHelper<DocumentationSetupHelper.SetupContext,
				SimpleSessionFactoryBuilder,
				SimpleSessionFactoryBuilder,
				SessionFactory> {

	public static List<? extends Arguments> testParamsForBothAnnotationsAndProgrammatic(
			Consumer<ProgrammaticMappingConfigurationContext> programmaticMappingContributor
	) {
		return testParamsForBothAnnotationsAndProgrammatic( Collections.emptySet(), programmaticMappingContributor );
	}

	public static List<? extends Arguments> testParamsForBothAnnotationsAndProgrammatic(
			Set<Class<?>> additionalAnnotatedClasses,
			Consumer<ProgrammaticMappingConfigurationContext> programmaticMappingContributor) {
		List<Arguments> result = new ArrayList<>();
		// Annotation-based mapping
		HibernateOrmSearchMappingConfigurer annotationMappingConfigurer =
				additionalAnnotatedClasses.isEmpty()
						? null
						: context -> context.annotationMapping().add( additionalAnnotatedClasses );
		result.add( Arguments.of( null, annotationMappingConfigurer ) );
		// Programmatic mapping
		HibernateOrmSearchMappingConfigurer programmaticMappingConfigurer =
				context -> programmaticMappingContributor.accept( context.programmaticMapping() );
		result.add( Arguments.of( false, programmaticMappingConfigurer ) );
		return result;
	}

	public static DocumentationSetupHelper withSingleBackend(BackendConfiguration backendConfiguration) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				null, null
		);
	}

	public static DocumentationSetupHelper withSingleBackend(BackendConfiguration backendConfiguration,
			Boolean annotationProcessingEnabled, HibernateOrmSearchMappingConfigurer defaultMappingConfigurer) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				annotationProcessingEnabled, defaultMappingConfigurer
		);
	}

	public static DocumentationSetupHelper withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return new DocumentationSetupHelper(
				BackendSetupStrategy.withMultipleBackends( defaultBackendConfiguration, namedBackendConfigurations ),
				null, null
		);
	}

	private Boolean annotationProcessingEnabled;
	private HibernateOrmSearchMappingConfigurer defaultMappingConfigurer;
	private OrmAssertionHelper assertionHelper;

	private DocumentationSetupHelper(BackendSetupStrategy backendSetupStrategy,
			Boolean annotationProcessingEnabled,
			HibernateOrmSearchMappingConfigurer defaultMappingConfigurer) {
		super( backendSetupStrategy, Type.METHOD );
		this.annotationProcessingEnabled = annotationProcessingEnabled;
		this.defaultMappingConfigurer = defaultMappingConfigurer;
		this.assertionHelper = new OrmAssertionHelper( backendSetupStrategy );
	}

	public DocumentationSetupHelper withMappingConfigurer(HibernateOrmSearchMappingConfigurer defaultMappingConfigurer) {
		if ( this.defaultMappingConfigurer != null ) {
			throw new IllegalStateException();
		}
		this.defaultMappingConfigurer = defaultMappingConfigurer;

		return this;
	}

	public DocumentationSetupHelper withAnnotationProcessingEnabled(Boolean annotationProcessingEnabled) {
		this.annotationProcessingEnabled = annotationProcessingEnabled;

		return this;
	}

	@Override
	public String toString() {
		return super.toString()
				+ ( annotationProcessingEnabled == Boolean.FALSE ? " - programmatic mapping" : "" );
	}

	@Override
	public OrmAssertionHelper assertions() {
		return assertionHelper;
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext( annotationProcessingEnabled, defaultMappingConfigurer );
	}

	@Override
	protected void close(SessionFactory toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends
			MappingSetupHelper<SetupContext,
					SimpleSessionFactoryBuilder,
					SimpleSessionFactoryBuilder,
					SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext(Boolean annotationProcessingEnabled, HibernateOrmSearchMappingConfigurer defaultMappingConfigurer) {
			// Real backend => ensure we clean up everything before and after the tests
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
					SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP );
			// Override the indexing plan synchronization strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
					IndexingPlanSynchronizationStrategyNames.SYNC );
			// Set up default mapping if necessary
			if ( annotationProcessingEnabled != null ) {
				withProperty( HibernateOrmMapperSettings.MAPPING_PROCESS_ANNOTATIONS, annotationProcessingEnabled );
			}
			if ( defaultMappingConfigurer != null ) {
				withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, defaultMappingConfigurer );
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

		public SetupContext tenants(String... tenants) {
			withConfiguration( b -> MultitenancyTestHelper.enable( b, tenants ) );
			return thisAsC();
		}

		public SessionFactory setup(Class<?>... annotatedTypes) {
			return withConfiguration( builder -> builder.addAnnotatedClasses( Arrays.asList( annotatedTypes ) ) )
					.setup();
		}

		@Override
		protected SimpleSessionFactoryBuilder createBuilder() {
			return new SimpleSessionFactoryBuilder();
		}

		@Override
		protected void consumeBeforeBuildConfigurations(SimpleSessionFactoryBuilder builder,
				List<Consumer<SimpleSessionFactoryBuilder>> consumers) {
			consumers.forEach( c -> c.accept( builder ) );
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
