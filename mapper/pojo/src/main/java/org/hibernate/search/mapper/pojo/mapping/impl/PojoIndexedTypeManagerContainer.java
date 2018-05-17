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

public class PojoIndexedTypeManagerContainer {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, PojoIndexedTypeManager<?, ?, ?>> byIndexName;
	private final Map<Class<?>, PojoIndexedTypeManager<?, ?, ?>> byExactClass;
	private final Map<Class<?>, Set<? extends PojoIndexedTypeManager<?, ?, ?>>> bySuperClass;
	private final Set<PojoIndexedTypeManager<?, ?, ?>> all;

	private PojoIndexedTypeManagerContainer(Builder builder) {
		this.byIndexName = new HashMap<>( builder.byIndexName );
		this.byExactClass = new HashMap<>( builder.byExactClass );
		this.bySuperClass = new HashMap<>( builder.bySuperClass );
		this.bySuperClass.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactClass.values() ) );
	}

	public Optional<PojoIndexedTypeManager<?, ?, ?>> getByIndexName(String indexName) {
		return Optional.ofNullable( byIndexName.get( indexName ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<PojoIndexedTypeManager<?, E, ?>> getByExactClass(Class<E> clazz) {
		return Optional.ofNullable( (PojoIndexedTypeManager<?, E, ?>) byExactClass.get( clazz ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<Set<PojoIndexedTypeManager<?, ? extends E, ?>>> getAllBySuperClass(Class<E> clazz) {
		return Optional.ofNullable( (Set<PojoIndexedTypeManager<?, ? extends E, ?>>) bySuperClass.get( clazz ) );
	}

	public Set<PojoIndexedTypeManager<?, ?, ?>> getAll() {
		return all;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, PojoIndexedTypeManager<?, ?, ?>> byIndexName = new LinkedHashMap<>();
		private final Map<Class<?>, PojoIndexedTypeManager<?, ?, ?>> byExactClass = new LinkedHashMap<>();
		private final Map<Class<?>, Set<PojoIndexedTypeManager<?, ?, ?>>> bySuperClass = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void add(String indexName, PojoRawTypeModel<E> typeModel, PojoIndexedTypeManager<?, E, ?> typeManager) {
			byIndexName.put( indexName, typeManager );
			byExactClass.put( typeModel.getJavaClass(), typeManager );
			typeModel.getAscendingSuperTypes()
					.map( PojoRawTypeModel::getJavaClass )
					.forEach( clazz ->
							bySuperClass.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager )
					);
		}

		public PojoIndexedTypeManagerContainer build() {
			return new PojoIndexedTypeManagerContainer( this );
		}
	}

}
