/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingTypeContextProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContextProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.orm.spi.BatchTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HibernateOrmTypeContextContainer
		implements HibernateOrmListenerTypeContextProvider, HibernateOrmSessionTypeContextProvider,
		AutomaticIndexingTypeContextProvider, BatchTypeContextProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final KeyValueProvider<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedByTypeIdentifier;
	private final KeyValueProvider<Class<?>, AbstractHibernateOrmTypeContext<?>> byExactClass;
	private final KeyValueProvider<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedByExactClass;
	private final KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byEntityName;
	private final KeyValueProvider<String, HibernateOrmIndexedTypeContext<?>> indexedByEntityName;
	private final KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byHibernateOrmEntityName;

	private HibernateOrmTypeContextContainer(Builder builder, PojoLoadingTypeContextProvider delegate,
			SessionFactoryImplementor sessionFactory) {
		// Use a LinkedHashMap for deterministic iteration
		Map<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedByTypeIdentifierContent = new LinkedHashMap<>();
		Map<Class<?>, AbstractHibernateOrmTypeContext<?>> byExactClassContent = new LinkedHashMap<>();
		Map<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedByExactClassContent = new LinkedHashMap<>();
		Map<String, AbstractHibernateOrmTypeContext<?>> byEntityNameContent = new LinkedHashMap<>();
		Map<String, HibernateOrmIndexedTypeContext<?>> indexedByEntityNameContent = new LinkedHashMap<>();
		Map<String, AbstractHibernateOrmTypeContext<?>> byHibernateOrmEntityNameContent = new LinkedHashMap<>();
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexed ) {
			HibernateOrmIndexedTypeContext<?> typeContext = contextBuilder.build( delegate, sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			indexedByTypeIdentifierContent.put( typeIdentifier, typeContext );

			if ( !typeIdentifier.isNamed() ) {
				byExactClassContent.put( typeIdentifier.javaClass(), typeContext );
				indexedByExactClassContent.put( typeIdentifier.javaClass(), typeContext );
			}

			byEntityNameContent.put( typeContext.jpaEntityName(), typeContext );
			indexedByEntityNameContent.put( typeContext.jpaEntityName(), typeContext );
			// Use putIfAbsent here to avoid overriding JPA entity names,
			// see org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver.Builder.addByName
			byEntityNameContent.putIfAbsent( typeContext.hibernateOrmEntityName(), typeContext );
			indexedByEntityNameContent.putIfAbsent( typeContext.hibernateOrmEntityName(), typeContext );

			byHibernateOrmEntityNameContent.put( typeContext.hibernateOrmEntityName(), typeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.contained ) {
			HibernateOrmContainedTypeContext<?> typeContext = contextBuilder.build( delegate, sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			if ( !typeIdentifier.isNamed() ) {
				byExactClassContent.put( typeIdentifier.javaClass(), typeContext );
			}

			byEntityNameContent.put( typeContext.jpaEntityName(), typeContext );
			// Use putIfAbsent here to avoid overriding JPA entity names,
			// see org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver.Builder.addByName
			byEntityNameContent.putIfAbsent( typeContext.hibernateOrmEntityName(), typeContext );

			byHibernateOrmEntityNameContent.put( typeContext.hibernateOrmEntityName(), typeContext );
		}
		this.indexedByTypeIdentifier =
				new KeyValueProvider<>( indexedByTypeIdentifierContent, log::unknownTypeIdentifierForIndexedEntityType );
		this.byExactClass = new KeyValueProvider<>( byExactClassContent, log::unknownClassForMappedEntityType );
		this.indexedByExactClass = new KeyValueProvider<>( indexedByExactClassContent, log::unknownClassForIndexedEntityType );
		this.byEntityName = new KeyValueProvider<>( byEntityNameContent, log::unknownEntityNameForMappedEntityType );
		this.indexedByEntityName =
				new KeyValueProvider<>( indexedByEntityNameContent, log::unknownEntityNameForIndexedEntityType );
		this.byHibernateOrmEntityName = new KeyValueProvider<>( byHibernateOrmEntityNameContent,
				log::unknownHibernateOrmEntityNameForMappedEntityType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (HibernateOrmIndexedTypeContext<E>) indexedByTypeIdentifier.getOrFail( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractHibernateOrmTypeContext<E> forExactClass(Class<E> typeIdentifier) {
		return (AbstractHibernateOrmTypeContext<E>) byExactClass.getOrFail( typeIdentifier );
	}

	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> indexedForExactClass(Class<E> typeIdentifier) {
		return (HibernateOrmIndexedTypeContext<E>) indexedByExactClass.getOrFail( typeIdentifier );
	}

	@Override
	public KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byEntityName() {
		return byEntityName;
	}

	public KeyValueProvider<String, HibernateOrmIndexedTypeContext<?>> indexedByEntityName() {
		return indexedByEntityName;
	}

	@Override
	public KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byHibernateOrmEntityName() {
		return byHibernateOrmEntityName;
	}

	Collection<? extends HibernateOrmIndexedTypeContext<?>> allIndexed() {
		return indexedByTypeIdentifier.values();
	}

	static class Builder {

		private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
		public final List<HibernateOrmIndexedTypeContext.Builder<?>> indexed = new ArrayList<>();
		public final List<HibernateOrmContainedTypeContext.Builder<?>> contained = new ArrayList<>();

		Builder(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider) {
			this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			String hibernateOrmEntityName = basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName );
			HibernateOrmIndexedTypeContext.Builder<E> builder = new HibernateOrmIndexedTypeContext.Builder<>( typeModel,
					basicTypeMetadataProvider.getPersistentClass( hibernateOrmEntityName ) );
			indexed.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			String hibernateOrmEntityName = basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName );
			HibernateOrmContainedTypeContext.Builder<E> builder = new HibernateOrmContainedTypeContext.Builder<>( typeModel,
					basicTypeMetadataProvider.getPersistentClass( hibernateOrmEntityName ) );
			contained.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(PojoLoadingTypeContextProvider delegate,
				SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, delegate, sessionFactory );
		}
	}

}
