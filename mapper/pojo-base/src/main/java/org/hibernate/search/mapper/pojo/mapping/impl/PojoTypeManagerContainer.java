/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContextProvider;
import org.hibernate.search.util.common.impl.Closer;

public class PojoTypeManagerContainer
		implements AutoCloseable, PojoWorkTypeContextProvider, PojoScopeTypeContextProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> indexedByExactType;
	private final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeManager<?, ?>> containedByExactType;
	private final Map<PojoRawTypeIdentifier<?>, Set<? extends PojoIndexedTypeManager<?, ?>>> indexedBySuperType;
	private final Map<String, PojoIndexedTypeManager<?, ?>> indexedByEntityName;
	private final Map<String, PojoContainedTypeManager<?, ?>> containedByEntityName;
	private final Set<PojoIndexedTypeManager<?, ?>> allIndexed;

	private PojoTypeManagerContainer(Builder builder) {
		// Use a LinkedHashMap for deterministic iteration in the "all" set
		this.indexedByExactType = new LinkedHashMap<>( builder.indexedByExactType );
		this.containedByExactType = new LinkedHashMap<>( builder.containedByExactType );
		this.indexedBySuperType = new LinkedHashMap<>( builder.indexedBySuperType );
		this.indexedBySuperType.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.indexedByEntityName = new LinkedHashMap<>( builder.indexedByExactType.size() );
		for ( PojoIndexedTypeManager<?, ?> typeManager : builder.indexedByExactType.values() ) {
			indexedByEntityName.put( typeManager.entityName(), typeManager );
		}
		this.containedByEntityName = new LinkedHashMap<>( builder.containedByExactType.size() );
		for ( PojoContainedTypeManager<?, ?> typeManager : builder.containedByExactType.values() ) {
			containedByEntityName.put( typeManager.entityName(), typeManager );
		}
		this.allIndexed = Collections.unmodifiableSet( new LinkedHashSet<>( indexedByExactType.values() ) );
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( AbstractPojoTypeManager::close, allIndexed );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends PojoIndexedTypeManager<?, E>> indexedForExactType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (PojoIndexedTypeManager<?, E>) indexedByExactType.get( typeIdentifier ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> allIndexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (Set<PojoIndexedTypeManager<?, ? extends E>>) indexedBySuperType.get( typeIdentifier ) );
	}

	@Override
	public Optional<? extends PojoIndexedTypeManager<?, ?>> indexedForEntityName(String entityName) {
		return Optional.ofNullable( (PojoIndexedTypeManager<?, ?>) indexedByEntityName.get( entityName ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends PojoContainedTypeManager<?, E>> containedForExactType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (PojoContainedTypeManager<?, E>) containedByExactType.get( typeIdentifier ) );
	}

	@Override
	public Optional<? extends PojoContainedTypeManager<?, ?>> containedForEntityName(String entityName) {
		return Optional.ofNullable( (PojoContainedTypeManager<?, ?>) containedByEntityName.get( entityName ) );
	}

	Set<PojoIndexedTypeManager<?, ?>> allIndexed() {
		return allIndexed;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> indexedByExactType = new LinkedHashMap<>();
		private final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeManager<?, ?>> containedByExactType = new LinkedHashMap<>();
		private final Map<PojoRawTypeIdentifier<?>, Set<PojoIndexedTypeManager<?, ?>>> indexedBySuperType = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void addIndexed(PojoRawTypeModel<E> typeModel, PojoIndexedTypeManager<?, E> typeManager) {
			indexedByExactType.put( typeModel.typeIdentifier(), typeManager );
			typeModel.ascendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.forEach( clazz ->
							indexedBySuperType.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager )
					);
		}

		public <E> void addContained(PojoRawTypeModel<E> typeModel, PojoContainedTypeManager<?, E> typeManager) {
			containedByExactType.put( typeModel.typeIdentifier(), typeManager );
		}

		public void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( PojoIndexedTypeManager::close, indexedByExactType.values() );
				closer.pushAll( PojoContainedTypeManager::close, containedByExactType.values() );
			}
		}

		public PojoTypeManagerContainer build() {
			return new PojoTypeManagerContainer( this );
		}
	}

}
