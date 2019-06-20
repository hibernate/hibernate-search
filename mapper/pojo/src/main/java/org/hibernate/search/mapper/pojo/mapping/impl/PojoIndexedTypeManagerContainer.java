/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContextProvider;
import org.hibernate.search.util.common.impl.Closer;

public class PojoIndexedTypeManagerContainer
		implements PojoWorkIndexedTypeContextProvider, PojoScopeIndexedTypeContextProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<Class<?>, PojoIndexedTypeManager<?, ?, ?>> byExactClass;
	private final Map<Class<?>, Set<? extends PojoIndexedTypeManager<?, ?, ?>>> bySuperClass;
	private final Set<PojoIndexedTypeManager<?, ?, ?>> all;

	private PojoIndexedTypeManagerContainer(Builder builder) {
		this.byExactClass = new HashMap<>( builder.byExactClass );
		this.bySuperClass = new HashMap<>( builder.bySuperClass );
		this.bySuperClass.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactClass.values() ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<PojoIndexedTypeManager<?, E, ?>> getByExactClass(Class<E> clazz) {
		return Optional.ofNullable( (PojoIndexedTypeManager<?, E, ?>) byExactClass.get( clazz ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<Set<PojoIndexedTypeManager<?, ? extends E, ?>>> getAllBySuperClass(Class<E> clazz) {
		return Optional.ofNullable( (Set<PojoIndexedTypeManager<?, ? extends E, ?>>) bySuperClass.get( clazz ) );
	}

	Set<PojoIndexedTypeManager<?, ?, ?>> getAll() {
		return all;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<Class<?>, PojoIndexedTypeManager<?, ?, ?>> byExactClass = new LinkedHashMap<>();
		private final Map<Class<?>, Set<PojoIndexedTypeManager<?, ?, ?>>> bySuperClass = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void add(PojoRawTypeModel<E> typeModel, PojoIndexedTypeManager<?, E, ?> typeManager) {
			byExactClass.put( typeModel.getJavaClass(), typeManager );
			typeModel.getAscendingSuperTypes()
					.map( PojoRawTypeModel::getJavaClass )
					.forEach( clazz ->
							bySuperClass.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager )
					);
		}

		public void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( PojoIndexedTypeManager::close, byExactClass.values() );
			}
		}

		public PojoIndexedTypeManagerContainer build() {
			return new PojoIndexedTypeManagerContainer( this );
		}
	}

}
