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
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContextProvider;
import org.hibernate.search.util.common.impl.Closer;

public class PojoIndexedTypeManagerContainer
		implements PojoWorkIndexedTypeContextProvider, PojoScopeIndexedTypeContextProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> byExactType;
	private final Map<PojoRawTypeIdentifier<?>, Set<? extends PojoIndexedTypeManager<?, ?>>> bySuperType;
	private final Set<PojoIndexedTypeManager<?, ?>> all;

	private PojoIndexedTypeManagerContainer(Builder builder) {
		// Use a LinkedHashMap for deterministic iteration in the "all" set
		this.byExactType = new LinkedHashMap<>( builder.byExactType );
		this.bySuperType = new LinkedHashMap<>( builder.bySuperType );
		this.bySuperType.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactType.values() ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends PojoWorkIndexedTypeContext<?, E>> getByExactType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (PojoIndexedTypeManager<?, E>) byExactType.get( typeIdentifier ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> getAllBySuperType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (Set<PojoIndexedTypeManager<?, ? extends E>>) bySuperType.get( typeIdentifier ) );
	}

	Set<PojoIndexedTypeManager<?, ?>> getAll() {
		return all;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeManager<?, ?>> byExactType = new LinkedHashMap<>();
		private final Map<PojoRawTypeIdentifier<?>, Set<PojoIndexedTypeManager<?, ?>>> bySuperType = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void add(PojoRawTypeModel<E> typeModel, PojoIndexedTypeManager<?, E> typeManager) {
			byExactType.put( typeModel.typeIdentifier(), typeManager );
			typeModel.ascendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.forEach( clazz ->
							bySuperType.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager )
					);
		}

		public void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( PojoIndexedTypeManager::close, byExactType.values() );
			}
		}

		public PojoIndexedTypeManagerContainer build() {
			return new PojoIndexedTypeManagerContainer( this );
		}
	}

}
