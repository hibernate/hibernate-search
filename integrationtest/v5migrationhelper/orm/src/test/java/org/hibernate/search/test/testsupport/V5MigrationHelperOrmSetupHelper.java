/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.testsupport;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.configuration.V5MigrationHelperTestLuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl.MultitenancyTestHelper;

public final class V5MigrationHelperOrmSetupHelper
		extends
		MappingSetupHelper<V5MigrationHelperOrmSetupHelper.SetupContext,
				SimpleSessionFactoryBuilder,
				SimpleSessionFactoryBuilder,
				SessionFactory> {

	public static V5MigrationHelperOrmSetupHelper create() {
		return new V5MigrationHelperOrmSetupHelper(
				BackendSetupStrategy.withSingleBackend( new V5MigrationHelperTestLuceneBackendConfiguration() )
		);
	}

	private final OrmAssertionHelper assertionHelper;

	private V5MigrationHelperOrmSetupHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
		this.assertionHelper = new OrmAssertionHelper( backendSetupStrategy );
	}

	@Override
	public OrmAssertionHelper assertions() {
		return assertionHelper;
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
			extends
			MappingSetupHelper<SetupContext,
					SimpleSessionFactoryBuilder,
					SimpleSessionFactoryBuilder,
					SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext() {
			// Search 5 used to do this, though it shouldn't matter.
			withProperty( BackendSettings.backendKey( LuceneBackendSettings.LUCENE_VERSION ),
					TestConstants.getTargetLuceneVersion().toString() );
			// Real backend => ensure we clean up everything before and after the tests
			withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
					SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP );
			// Override the automatic indexing synchronization strategy according to our needs for testing
			withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
					AutomaticIndexingSynchronizationStrategyNames.SYNC );
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
