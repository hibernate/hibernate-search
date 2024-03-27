/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSearchSessionTypeContextProvider;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class StandalonePojoTypeContextContainer
		implements StandalonePojoSearchSessionTypeContextProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final KeyValueProvider<PojoRawTypeIdentifier<?>, AbstractStandalonePojoTypeContext<?>> byTypeIdentifier;
	private final KeyValueProvider<PojoRawTypeIdentifier<?>, StandalonePojoIndexedTypeContext<?>> indexedByTypeIdentifier;
	private final KeyValueProvider<Class<?>, AbstractStandalonePojoTypeContext<?>> byExactClass;
	private final KeyValueProvider<Class<?>, StandalonePojoIndexedTypeContext<?>> indexedByExactClass;
	private final KeyValueProvider<String, StandalonePojoIndexedTypeContext<?>> indexedByEntityName;

	private StandalonePojoTypeContextContainer(Builder builder) {
		// Use a LinkedHashMap for deterministic iteration
		Map<PojoRawTypeIdentifier<?>, AbstractStandalonePojoTypeContext<?>> byTypeIdentifierContent = new LinkedHashMap<>();
		Map<PojoRawTypeIdentifier<?>, StandalonePojoIndexedTypeContext<?>> indexedByTypeIdentifierContent =
				new LinkedHashMap<>();
		Map<Class<?>, AbstractStandalonePojoTypeContext<?>> byExactClassContent = new LinkedHashMap<>();
		Map<Class<?>, StandalonePojoIndexedTypeContext<?>> indexedByExactClassContent = new LinkedHashMap<>();
		Map<String, StandalonePojoIndexedTypeContext<?>> indexedByEntityNameContent = new LinkedHashMap<>();

		for ( StandalonePojoIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			StandalonePojoIndexedTypeContext<?> typeContext = contextBuilder.build();
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			byTypeIdentifierContent.put( typeIdentifier, typeContext );
			indexedByTypeIdentifierContent.put( typeIdentifier, typeContext );

			byExactClassContent.put( typeContext.javaClass(), typeContext );
			indexedByExactClassContent.put( typeContext.javaClass(), typeContext );

			indexedByEntityNameContent.put( typeContext.name(), typeContext );
		}
		for ( StandalonePojoContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			StandalonePojoContainedTypeContext<?> typeContext = contextBuilder.build();
			PojoRawTypeIdentifier<?> typeIdentifier = typeContext.typeIdentifier();

			byTypeIdentifierContent.put( typeIdentifier, typeContext );

			byExactClassContent.put( typeContext.javaClass(), typeContext );
		}
		this.byTypeIdentifier =
				new KeyValueProvider<>( byTypeIdentifierContent, log::unknownTypeIdentifierForMappedEntityType );
		this.indexedByTypeIdentifier =
				new KeyValueProvider<>( indexedByTypeIdentifierContent, log::unknownTypeIdentifierForIndexedEntityType );
		this.byExactClass = new KeyValueProvider<>( byExactClassContent, log::unknownClassForMappedEntityType );
		this.indexedByExactClass = new KeyValueProvider<>( indexedByExactClassContent, log::unknownClassForIndexedEntityType );
		this.indexedByEntityName =
				new KeyValueProvider<>( indexedByEntityNameContent, log::unknownEntityNameForIndexedEntityType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractStandalonePojoTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractStandalonePojoTypeContext<E>) byTypeIdentifier.getOrFail( typeIdentifier );
	}

	@SuppressWarnings("unchecked")
	public <E> StandalonePojoIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (StandalonePojoIndexedTypeContext<E>) indexedByTypeIdentifier.getOrFail( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractStandalonePojoTypeContext<E> forExactClass(Class<E> clazz) {
		return (AbstractStandalonePojoTypeContext<E>) byExactClass.getOrFail( clazz );
	}

	@SuppressWarnings("unchecked")
	public <E> StandalonePojoIndexedTypeContext<E> indexedForExactClass(Class<E> clazz) {
		return (StandalonePojoIndexedTypeContext<E>) indexedByExactClass.getOrFail( clazz );
	}

	@Override
	public KeyValueProvider<String, StandalonePojoIndexedTypeContext<?>> indexedByEntityName() {
		return indexedByEntityName;
	}

	public Collection<? extends StandalonePojoIndexedTypeContext<?>> allIndexed() {
		return indexedByTypeIdentifier.values();
	}

	static class Builder {

		private final List<StandalonePojoIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<StandalonePojoContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		<E> StandalonePojoIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String entityName) {
			StandalonePojoIndexedTypeContext.Builder<E> builder =
					new StandalonePojoIndexedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> StandalonePojoContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String entityName) {
			StandalonePojoContainedTypeContext.Builder<E> builder =
					new StandalonePojoContainedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName );
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		StandalonePojoTypeContextContainer build() {
			return new StandalonePojoTypeContextContainer( this );
		}
	}

}
