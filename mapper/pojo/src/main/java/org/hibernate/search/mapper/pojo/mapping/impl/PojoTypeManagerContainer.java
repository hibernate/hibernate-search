/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoIndexableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeManagerContainer {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, PojoTypeManager<?, ?, ?>> byIndexName;
	private final Map<Class<?>, PojoTypeManager<?, ?, ?>> byExactClass;
	private final Map<Class<?>, Set<? extends PojoTypeManager<?, ?, ?>>> bySuperClass;
	private final Set<PojoTypeManager<?, ?, ?>> all;

	private PojoTypeManagerContainer(Builder builder) {
		this.byIndexName = new HashMap<>( builder.byIndexName );
		this.byExactClass = new HashMap<>( builder.byExactClass );
		this.bySuperClass = new HashMap<>( builder.bySuperClass );
		this.bySuperClass.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactClass.values() ) );
	}

	public Optional<PojoTypeManager<?, ?, ?>> getByIndexName(String indexName) {
		return Optional.ofNullable( byIndexName.get( indexName ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<PojoTypeManager<?, E, ?>> getByExactClass(Class<E> clazz) {
		return Optional.ofNullable( (PojoTypeManager<?, E, ?>) byExactClass.get( clazz ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<Set<PojoTypeManager<?, ? extends E, ?>>> getAllBySuperClass(Class<E> clazz) {
		return Optional.ofNullable( (Set<PojoTypeManager<?, ? extends E, ?>>) bySuperClass.get( clazz ) );
	}

	public Set<PojoTypeManager<?, ?, ?>> getAll() {
		return all;
	}

	public static class Builder {

		private final Map<String, PojoTypeManager<?, ?, ?>> byIndexName = new HashMap<>();
		private final Map<Class<?>, PojoTypeManager<?, ?, ?>> byExactClass = new HashMap<>();
		private final Map<Class<?>, Set<PojoTypeManager<?, ?, ?>>> bySuperClass = new HashMap<>();

		private Builder() {
		}

		public <E> void add(String indexName, PojoIndexableTypeModel<E> typeModel, PojoTypeManager<?, E, ?> typeManager) {
			byIndexName.put( indexName, typeManager );
			byExactClass.put( typeModel.getJavaClass(), typeManager );
			typeModel.getAscendingSuperTypes()
					.map( PojoTypeModel::getJavaClass )
					.forEach( clazz ->
							bySuperClass.computeIfAbsent( clazz, ignored -> new LinkedHashSet<>() )
									.add( typeManager )
					);
		}

		public PojoTypeManagerContainer build() {
			return new PojoTypeManagerContainer( this );
		}
	}

}
