/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.impl.test.extension.ExtensionScope;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.jboss.logging.Logger;

/**
 * This cleaner will look for a method annotated with a @DataClearConfigConfigurer
 * If such method is found it assumes that data must be cleaned up according to the config after each test method execution.
 */
class OrmSetupHelperCleaner {
	private static final Logger log = Logger.getLogger( OrmSetupHelperCleaner.class.getName() );

	private final DataClearConfigImpl config;
	private final SessionFactoryImplementor sessionFactory;

	static OrmSetupHelperCleaner create(SessionFactoryImplementor sessionFactory, ExtensionScope scope, boolean mockBackend) {
		// if we have a test scope cleaner we don't need to clean the data as the session factory will be closed anyway.
		if ( !ExtensionScope.TEST.equals( scope ) ) {
			return new OrmSetupHelperCleaner( sessionFactory ).appendConfiguration(
					config -> config.clearDatabaseData( true ).clearIndexData( !mockBackend ) );
		}
		return new OrmSetupHelperCleaner( sessionFactory );
	}

	private OrmSetupHelperCleaner(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.config = new DataClearConfigImpl();
	}

	void cleanupData() {
		if ( !( !DataClearConfig.ClearDatabaseData.DISABLED.equals( config.clearDatabaseData ) || config.clearIndexData ) ) {
			return;
		}
		log.info( "Clearing data and reusing the same session factory." );
		try {
			clearAllData( sessionFactory );
		}
		catch (RuntimeException e) {
			// Close the session factory (and consequently drop the schema) so that later tests
			// are not affected by the failure.
			new SuppressingCloser( e )
					.push( () -> this.tearDownSessionFactory( sessionFactory ) );
			throw new Error( "Failed to clear data before test execution: " + e.getMessage(), e );
		}
	}

	private void tearDownSessionFactory(SessionFactoryImplementor sessionFactory) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SessionFactory::close, sessionFactory );
		}
	}

	private void clearAllData(SessionFactoryImplementor sessionFactory) {
		HibernateOrmMapping mapping = null;
		try {
			mapping = ( (HibernateOrmMapping) Search.mapping( sessionFactory ) );
		}
		catch (SearchException e) {
			if ( !e.getMessage().contains( "not initialized" ) ) {
				throw e;
			}
			// Else: Hibernate Search is simply disabled.
		}

		if ( !DataClearConfig.ClearDatabaseData.DISABLED.equals( this.config.clearDatabaseData ) ) {
			if ( DataClearConfig.ClearDatabaseData.AUTOMATIC.equals( this.config.clearDatabaseData ) ) {
				sessionFactory.getCache().evictAllRegions();

				clearDatabase( sessionFactory, mapping );
			}
			else {
				manualClearDatabase( sessionFactory, mapping );
			}
		}

		if ( mapping != null && this.config.clearIndexData ) {
			Search.mapping( sessionFactory ).scope( Object.class ).schemaManager().dropAndCreate();
		}
	}

	private void manualClearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping) {
		if ( config.tenantIds.isEmpty() ) {
			executeActions( config.manual, sessionFactory, mapping, null );
		}
		else {
			for ( Object tenantsId : config.tenantIds ) {
				executeActions( config.manual, sessionFactory, mapping, tenantsId );
			}
		}
	}

	private void executeActions(List<ThrowingConsumer<Session, RuntimeException>> actions,
			SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping, Object tenantId) {
		if ( mapping != null ) {
			mapping.listenerEnabled( false );
		}
		for ( ThrowingConsumer<Session, RuntimeException> action : actions ) {
			//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
			OrmUtils.with( sessionFactory, tenantId ).runInTransaction( action );
			//CHECKSTYLE:ON
		}
		if ( mapping != null ) {
			mapping.listenerEnabled( true );
		}
	}

	private void clearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping) {
		if ( config.tenantIds.isEmpty() ) {
			executeActions( config.preClear, sessionFactory, mapping, null );
		}
		else {
			for ( Object tenantId : config.tenantIds ) {
				executeActions( config.preClear, sessionFactory, mapping, tenantId );
			}
		}

		sessionFactory.getSchemaManager().truncateMappedObjects();
	}

	public OrmSetupHelperCleaner appendConfiguration(Consumer<DataClearConfig> configurer) {
		configurer.accept( this.config );
		return this;
	}

	public boolean usesExactly(SessionFactory sessionFactory) {
		// exactly the same
		return this.sessionFactory == sessionFactory;
	}

	private static class DataClearConfigImpl implements DataClearConfig {
		private final List<Object> tenantIds = new ArrayList<>();

		private final List<ThrowingConsumer<Session, RuntimeException>> preClear = new ArrayList<>();
		private final List<ThrowingConsumer<Session, RuntimeException>> manual = new ArrayList<>();

		private boolean clearIndexData = false;
		private ClearDatabaseData clearDatabaseData = ClearDatabaseData.DISABLED;

		@Override
		public DataClearConfig clearDatabaseData(ClearDatabaseData clear) {
			this.clearDatabaseData = clear;
			return this;
		}

		@Override
		public DataClearConfig tenants(Object... tenantIds) {
			Collections.addAll( this.tenantIds, tenantIds );
			return this;
		}

		@Override
		public DataClearConfig preClear(Consumer<Session> preClear) {
			this.preClear.add( preClear::accept );
			return this;
		}

		@Override
		public <T> DataClearConfig preClear(Class<T> entityType, Consumer<T> preClear) {
			return preClear( session -> {
				// We'll go through subtypes as well here,
				// on contrary to selectAllOfSpecificType(),
				// because we are performing updates only, not deletes.

				CriteriaBuilder builder = session.getCriteriaBuilder();
				CriteriaQuery<T> query = builder.createQuery( entityType );
				Root<T> root = query.from( entityType );
				query.select( root );
				for ( T entity : session.createQuery( query ).list() ) {
					preClear.accept( entity );
				}
			} );
		}

		@Override
		public DataClearConfig clearIndexData(boolean clear) {
			this.clearIndexData = clear;
			return this;
		}

		@Override
		public DataClearConfig manualDatabaseCleanup(Consumer<Session> cleanupAction) {
			this.manual.add( cleanupAction::accept );
			return this;
		}
	}

}
