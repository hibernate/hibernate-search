/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.orm.loading.impl.LoadingIndexedTypeContextProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContextProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.orm.spi.BatchTypeIdentifierProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HibernateOrmTypeContextContainer
		implements HibernateOrmListenerTypeContextProvider, HibernateOrmSessionTypeContextProvider,
				AutomaticIndexingTypeContextProvider, LoadingIndexedTypeContextProvider, BatchTypeIdentifierProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmRawTypeIdentifierResolver typeIdentifierResolver;

	private final KeyValueProvider<PojoRawTypeIdentifier<?>, AbstractHibernateOrmTypeContext<?>> byTypeIdentifier;
	private final KeyValueProvider<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedByTypeIdentifier;
	private final KeyValueProvider<Class<?>, AbstractHibernateOrmTypeContext<?>> byExactClass;
	private final KeyValueProvider<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedByExactClass;
	private final KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byEntityName;
	private final KeyValueProvider<String, HibernateOrmIndexedTypeContext<?>> indexedByEntityName;
	private final KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byHibernateOrmEntityName;
	private final KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byJpaEntityName;
	private final KeyValueProvider<String, HibernateOrmIndexedTypeContext<?>> indexedByJpaEntityName;

	private HibernateOrmTypeContextContainer(Builder builder, SessionFactoryImplementor sessionFactory) {
		this.typeIdentifierResolver = builder.basicTypeMetadataProvider.getTypeIdentifierResolver();
		// Use a LinkedHashMap for deterministic iteration
		Map<PojoRawTypeIdentifier<?>, AbstractHibernateOrmTypeContext<?>> byTypeIdentifierContent = new LinkedHashMap<>();
		Map<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedByTypeIdentifierContent = new LinkedHashMap<>();
		Map<Class<?>, AbstractHibernateOrmTypeContext<?>> byExactClassContent = new LinkedHashMap<>();
		Map<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedByExactClassContent = new LinkedHashMap<>();
		Map<String, AbstractHibernateOrmTypeContext<?>> byEntityNameContent = new LinkedHashMap<>();
		Map<String, HibernateOrmIndexedTypeContext<?>> indexedByEntityNameContent = new LinkedHashMap<>();
		Map<String, AbstractHibernateOrmTypeContext<?>> byJpaEntityNameContent = new LinkedHashMap<>();
		Map<String, HibernateOrmIndexedTypeContext<?>> indexedByJpaEntityNameContent = new LinkedHashMap<>();
		Map<String, AbstractHibernateOrmTypeContext<?>> byHibernateOrmEntityNameContent = new LinkedHashMap<>();
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			HibernateOrmIndexedTypeContext<?> typeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			byTypeIdentifierContent.put( typeIdentifier, typeContext );
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

			byJpaEntityNameContent.put( typeContext.jpaEntityName(), typeContext );
			indexedByJpaEntityNameContent.put( typeContext.jpaEntityName(), typeContext );

			byHibernateOrmEntityNameContent.put( typeContext.hibernateOrmEntityName(), typeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			HibernateOrmContainedTypeContext<?> typeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			byTypeIdentifierContent.put( typeIdentifier, typeContext );

			if ( !typeIdentifier.isNamed() ) {
				byExactClassContent.put( typeIdentifier.javaClass(), typeContext );
			}

			byEntityNameContent.put( typeContext.jpaEntityName(), typeContext );
			// Use putIfAbsent here to avoid overriding JPA entity names,
			// see org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver.Builder.addByName
			byEntityNameContent.putIfAbsent( typeContext.hibernateOrmEntityName(), typeContext );

			byJpaEntityNameContent.put( typeContext.jpaEntityName(), typeContext );

			byHibernateOrmEntityNameContent.put( typeContext.hibernateOrmEntityName(), typeContext );
		}
		this.byTypeIdentifier = new KeyValueProvider<>( byTypeIdentifierContent, log::unknownTypeIdentifierForMappedEntityType );
		this.indexedByTypeIdentifier = new KeyValueProvider<>( indexedByTypeIdentifierContent, log::unknownTypeIdentifierForIndexedEntityType );
		this.byExactClass = new KeyValueProvider<>( byExactClassContent, log::unknownClassForMappedEntityType );
		this.indexedByExactClass = new KeyValueProvider<>( indexedByExactClassContent, log::unknownClassForIndexedEntityType );
		this.byEntityName = new KeyValueProvider<>( byEntityNameContent, log::unknownEntityNameForMappedEntityType );
		this.indexedByEntityName = new KeyValueProvider<>( indexedByEntityNameContent, log::unknownEntityNameForIndexedEntityType );
		this.byJpaEntityName = new KeyValueProvider<>( byJpaEntityNameContent, log::unknownJpaEntityNameForMappedEntityType );
		this.indexedByJpaEntityName = new KeyValueProvider<>( indexedByJpaEntityNameContent, log::unknownJpaEntityNameForIndexedEntityType );
		this.byHibernateOrmEntityName = new KeyValueProvider<>( byHibernateOrmEntityNameContent, log::unknownHibernateOrmEntityNameForMappedEntityType );
	}

	@Override
	public HibernateOrmRawTypeIdentifierResolver typeIdentifierResolver() {
		return typeIdentifierResolver;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractHibernateOrmTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractHibernateOrmTypeContext<E>) byTypeIdentifier.getOrFail( typeIdentifier );
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
	public KeyValueProvider<String, AbstractHibernateOrmTypeContext<?>> byJpaEntityName() {
		return byJpaEntityName;
	}

	public KeyValueProvider<String, HibernateOrmIndexedTypeContext<?>> indexedByJpaEntityName() {
		return indexedByJpaEntityName;
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
		private final List<HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider) {
			this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			HibernateOrmIndexedTypeContext.Builder<E> builder = new HibernateOrmIndexedTypeContext.Builder<>(
					typeModel,
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName )
			);
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			HibernateOrmContainedTypeContext.Builder<E> builder = new HibernateOrmContainedTypeContext.Builder<>(
					typeModel,
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName )
			);
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, sessionFactory );
		}
	}

}
