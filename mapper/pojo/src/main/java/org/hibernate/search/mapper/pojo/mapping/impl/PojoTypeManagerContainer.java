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

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeOrdering;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeManagerContainer {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, PojoTypeManager<?, ?, ?>> byIndexName;
	private final Map<Class<?>, PojoTypeManager<?, ?, ?>> byExactType;
	private final Map<Class<?>, Set<? extends PojoTypeManager<?, ?, ?>>> bySuperType;
	private final Set<PojoTypeManager<?, ?, ?>> all;

	private PojoTypeManagerContainer(Builder builder) {
		this.byIndexName = new HashMap<>( builder.byIndexName );
		this.byExactType = new HashMap<>( builder.byExactType );
		this.bySuperType = new HashMap<>( builder.bySuperType );
		this.bySuperType.replaceAll( (k, v) -> Collections.unmodifiableSet( v ) );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactType.values() ) );
	}

	public Optional<PojoTypeManager<?, ?, ?>> getByIndexName(String indexName) {
		return Optional.ofNullable( byIndexName.get( indexName ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<PojoTypeManager<?, E, ?>> getByExactType(Class<E> type) {
		return Optional.ofNullable( (PojoTypeManager<?, E, ?>) byExactType.get( type ) );
	}

	@SuppressWarnings("unchecked")
	public <E> Optional<Set<PojoTypeManager<?, ? extends E, ?>>> getAllBySuperType(Class<E> type) {
		return Optional.ofNullable( (Set<PojoTypeManager<?, ? extends E, ?>>) bySuperType.get( type ) );
	}

	public Set<PojoTypeManager<?, ?, ?>> getAll() {
		return all;
	}

	public static class Builder {

		private final Map<String, PojoTypeManager<?, ?, ?>> byIndexName = new HashMap<>();
		private final Map<Class<?>, PojoTypeManager<?, ?, ?>> byExactType = new HashMap<>();
		private final Map<Class<?>, Set<PojoTypeManager<?, ?, ?>>> bySuperType = new HashMap<>();

		private Builder() {
		}

		public <E> void add(String indexName, Class<E> indexedType, PojoTypeManager<?, E, ?> typeManager) {
			byIndexName.put( indexName, typeManager );
			byExactType.put( indexedType, typeManager );
			PojoTypeOrdering.get().getAscendingSuperTypes( indexedType )
					.forEach( type -> bySuperType.computeIfAbsent( type, ignored -> new LinkedHashSet<>() ).add( typeManager ) );
		}

		public PojoTypeManagerContainer build() {
			return new PojoTypeManagerContainer( this );
		}
	}

}
