/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.standalone.loading.impl.LoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadata;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSearchSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class StandalonePojoTypeContextContainer
		implements StandalonePojoSearchSessionTypeContextProvider, LoadingTypeContextProvider {

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, StandalonePojoIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<Class<?>, StandalonePojoIndexedTypeContext<?>> indexedTypeContextsByClass = new LinkedHashMap<>();
	private final Map<String, StandalonePojoIndexedTypeContext<?>> indexedTypeContextsByEntityName = new LinkedHashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, AbstractStandalonePojoTypeContext<?>> typeContexts = new LinkedHashMap<>();

	private StandalonePojoTypeContextContainer(Builder builder) {
		for ( StandalonePojoIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			StandalonePojoIndexedTypeContext<?> typeContext = contextBuilder.build();
			indexedTypeContexts.put( typeContext.typeIdentifier(), typeContext );
			indexedTypeContextsByClass.put( typeContext.javaClass(), typeContext );
			indexedTypeContextsByEntityName.put( typeContext.name(), typeContext );
			typeContexts.put( typeContext.typeIdentifier(), typeContext );
		}
		for ( StandalonePojoContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			StandalonePojoContainedTypeContext<?> typeContext = contextBuilder.build();
			typeContexts.put( typeContext.typeIdentifier(), typeContext );
		}
	}

	@SuppressWarnings("unchecked")
	public <E> StandalonePojoIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (StandalonePojoIndexedTypeContext<E>) indexedTypeContexts.get( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractStandalonePojoTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractStandalonePojoTypeContext<E>) typeContexts.get( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> StandalonePojoIndexedTypeContext<E> indexedForExactClass(Class<E> clazz) {
		return (StandalonePojoIndexedTypeContext<E>) indexedTypeContextsByClass.get( clazz );
	}

	@Override
	public StandalonePojoIndexedTypeContext<?> indexedForEntityName(String indexName) {
		return indexedTypeContextsByEntityName.get( indexName );
	}

	public Collection<StandalonePojoIndexedTypeContext<?>> allIndexed() {
		return indexedTypeContexts.values();
	}

	static class Builder {

		private final List<StandalonePojoIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<StandalonePojoContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> StandalonePojoIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String entityName,
				StandalonePojoEntityTypeMetadata<E> metadata) {
			StandalonePojoIndexedTypeContext.Builder<E> builder =
					new StandalonePojoIndexedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName, metadata );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> StandalonePojoContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String entityName,
				StandalonePojoEntityTypeMetadata<E> metadata) {
			StandalonePojoContainedTypeContext.Builder<E> builder =
					new StandalonePojoContainedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName, metadata );
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		StandalonePojoTypeContextContainer build() {
			return new StandalonePojoTypeContextContainer( this );
		}
	}

}
