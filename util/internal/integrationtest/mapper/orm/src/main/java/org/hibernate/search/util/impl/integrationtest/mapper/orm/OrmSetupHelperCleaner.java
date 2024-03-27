/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
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
		if ( !( config.clearDatabaseData || config.clearIndexData ) ) {
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
		HibernateOrmMapping mapping;
		try {
			mapping = ( (HibernateOrmMapping) Search.mapping( sessionFactory ) );
		}
		catch (SearchException e) {
			if ( e.getMessage().contains( "not initialized" ) ) {
				// Hibernate Search is simply disabled.
				mapping = null;
			}
			else {
				throw e;
			}
		}

		if ( this.config.clearDatabaseData ) {
			sessionFactory.getCache().evictAllRegions();

			clearDatabase( sessionFactory, mapping );

			// Must re-clear the caches as they may have been re-populated
			// while executing queries in clearDatabase().
			sessionFactory.getCache().evictAllRegions();
		}

		if ( mapping != null && this.config.clearIndexData ) {
			Search.mapping( sessionFactory ).scope( Object.class ).schemaManager().dropAndCreate();
		}
	}

	private void clearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping) {
		if ( config.tenantsIds.isEmpty() ) {
			clearDatabase( sessionFactory, mapping, null );
		}
		else {
			for ( Object tenantsId : config.tenantsIds ) {
				clearDatabase( sessionFactory, mapping, tenantsId );
			}
		}
	}


	private void clearDatabase(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping, Object tenantId) {
		for ( ThrowingConsumer<Session, RuntimeException> preClear : config.preClear ) {
			if ( mapping != null ) {
				mapping.listenerEnabled( false );
			}
			//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
			OrmUtils.with( sessionFactory, tenantId ).runInTransaction( preClear );
			//CHECKSTYLE:ON
			if ( mapping != null ) {
				mapping.listenerEnabled( true );
			}
		}

		Set<String> clearedEntityNames = new HashSet<>();
		for ( Class<?> entityClass : config.entityClearOrder ) {
			EntityType<?> entityType;
			try {
				entityType = sessionFactory.getJpaMetamodel().entity( entityClass );
			}
			catch (IllegalArgumentException e) {
				// When using annotatedTypes to infer the clear order,
				// some annotated types may not be entities;
				// this can be ignored.
				continue;
			}
			if ( clearedEntityNames.add( entityType.getName() ) ) {
				clearEntityInstances( sessionFactory, mapping, tenantId, entityType );
			}
		}

		// Just in case some entity types were not mentioned in entityClearOrder,
		// we try to delete all remaining entity types.
		// Note we're stabilizing the order, because ORM uses a HashSet internally
		// and the order may change from one execution to the next.
		List<EntityType<?>> sortedEntityTypes = sessionFactory.getJpaMetamodel().getEntities().stream()
				.sorted( Comparator.comparing( EntityType::getName ) )
				.collect( Collectors.toList() );
		for ( EntityType<?> entityType : sortedEntityTypes ) {
			if ( clearedEntityNames.add( entityType.getName() ) ) {
				clearEntityInstances( sessionFactory, mapping, tenantId, entityType );
			}
		}
	}

	private static void clearEntityInstances(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping,
			Object tenantId, EntityType<?> entityType) {
		if ( Modifier.isAbstract( entityType.getJavaType().getModifiers() ) ) {
			// There are no instances of this specific class,
			// only instances of subclasses, and those are handled separately.
			return;
		}
		if ( hasSelfAssociation( entityType.getJavaType(), sessionFactory, entityType ) ) {
			if ( mapping != null ) {
				mapping.listenerEnabled( false );
			}
			try {
				//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
				OrmUtils.with( sessionFactory, tenantId ).runInTransaction( s -> {
					//CHECKSTYLE:ON
					Query<?> query = selectAllOfSpecificType( entityType, s );
					try {
						query.list().forEach( s::remove );
					}
					catch (RuntimeException e) {
						throw new RuntimeException( "Failed to delete all entity instances returned by "
								+ query.getQueryString() + " on type " + entityType + ": " + e.getMessage(), e );
					}
				} );
			}
			finally {
				if ( mapping != null ) {
					mapping.listenerEnabled( true );
				}
			}
		}
		else {
			//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
			OrmUtils.with( sessionFactory, tenantId ).runInTransaction( s -> {
				//CHECKSTYLE:ON
				Query<?> query = deleteAllOfSpecificType( entityType, s );
				try {
					query.executeUpdate();
				}
				catch (RuntimeException e) {
					throw new RuntimeException( "Failed to execute " + query.getQueryString() + " on type " + entityType
							+ ": " + e.getMessage(), e );
				}
			} );
		}
	}

	private static Query<?> selectAllOfSpecificType(EntityType<?> entityType, Session session) {
		return createSelectOrDeleteAllOfSpecificTypeQuery( entityType, session, QueryType.SELECT );
	}

	private static Query<?> deleteAllOfSpecificType(EntityType<?> entityType, Session session) {
		return createSelectOrDeleteAllOfSpecificTypeQuery( entityType, session, QueryType.DELETE );
	}

	public OrmSetupHelperCleaner appendConfiguration(Consumer<DataClearConfig> configurer) {
		configurer.accept( this.config );
		return this;
	}

	public boolean usesExactly(SessionFactory sessionFactory) {
		// exactly the same
		return this.sessionFactory == sessionFactory;
	}

	enum QueryType {
		SELECT,
		DELETE
	}

	private static Query<?> createSelectOrDeleteAllOfSpecificTypeQuery(EntityType<?> entityType, Session session,
			QueryType queryType) {
		StringBuilder builder = new StringBuilder( QueryType.SELECT.equals( queryType ) ? "select e " : "delete " );

		builder.append( "from " ).append( entityType.getName() ).append( " e" );
		Class<?> typeArg = null;
		if ( hasEntitySubclass( session.getSessionFactory(), entityType ) ) {
			// We must target the type explicitly, without polymorphism,
			// because subtypes might have associations pointing to the supertype,
			// in which case deleting subtypes and supertypes in the same query
			// may fail or not, depending on processing order (supertype before subtype or subtype before supertype).
			builder.append( " where type( e ) in (:type)" );
			typeArg = entityType.getJavaType();
		}
		@SuppressWarnings("deprecation")
		Query<?> query = QueryType.SELECT.equals( queryType )
				? session.createQuery( builder.toString(), entityType.getJavaType() )
				: session.createQuery( builder.toString() );
		if ( typeArg != null ) {
			query.setParameter( "type", typeArg );
		}
		return query;
	}

	private static boolean hasEntitySubclass(SessionFactory sessionFactory, EntityType<?> parentEntity) {
		Metamodel metamodel = sessionFactory.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel();
		for ( EntityType<?> entity : metamodel.getEntities() ) {
			if ( parentEntity.equals( entity.getSupertype() ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasSelfAssociation(Class<?> entityJavaType, SessionFactoryImplementor sessionFactory,
			ManagedType<?> managedType) {
		for ( Attribute<?, ?> attribute : managedType.getAttributes() ) {
			if ( attribute.isAssociation() ) {
				Class<?> type;
				if ( attribute.isCollection() ) {
					type = ( (PluralAttribute<?, ?, ?>) attribute ).getElementType().getJavaType();
				}
				else {
					type = attribute.getJavaType();
				}
				if ( entityJavaType.isAssignableFrom( type ) ) {
					return true;
				}
			}
			if ( Attribute.PersistentAttributeType.EMBEDDED.equals( attribute.getPersistentAttributeType() ) ) {
				EmbeddableType<?> embeddable = sessionFactory.getJpaMetamodel().embeddable( attribute.getJavaType() );
				if ( hasSelfAssociation( entityJavaType, sessionFactory, embeddable ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static class DataClearConfigImpl implements DataClearConfig {
		private final List<Object> tenantsIds = new ArrayList<>();

		private final List<Class<?>> entityClearOrder = new ArrayList<>();

		private final List<ThrowingConsumer<Session, RuntimeException>> preClear = new ArrayList<>();

		private boolean clearIndexData = false;
		private boolean clearDatabaseData = false;

		@Override
		public DataClearConfig clearDatabaseData(boolean clear) {
			this.clearDatabaseData = clear;
			return this;
		}

		@Override
		public DataClearConfig tenants(Object... tenantIds) {
			Collections.addAll( this.tenantsIds, tenantIds );
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
		public DataClearConfig clearOrder(Class<?>... entityClasses) {
			entityClearOrder.clear();
			Collections.addAll( entityClearOrder, entityClasses );
			return this;
		}

		@Override
		public DataClearConfig clearIndexData(boolean clear) {
			this.clearIndexData = clear;
			return this;
		}
	}

}
