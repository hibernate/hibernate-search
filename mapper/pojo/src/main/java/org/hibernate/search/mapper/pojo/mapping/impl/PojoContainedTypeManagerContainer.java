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
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContextProvider;
import org.hibernate.search.util.common.impl.Closer;

public class PojoContainedTypeManagerContainer implements PojoWorkContainedTypeContextProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<Class<?>, PojoContainedTypeManager<?>> byExactClass;
	private final Set<PojoContainedTypeManager<?>> all;

	private PojoContainedTypeManagerContainer(Builder builder) {
		this.byExactClass = new HashMap<>( builder.byExactClass );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactClass.values() ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<PojoContainedTypeManager<E>> getByExactClass(Class<E> clazz) {
		return Optional.ofNullable( (PojoContainedTypeManager<E>) byExactClass.get( clazz ) );
	}

	Set<PojoContainedTypeManager<?>> getAll() {
		return all;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<Class<?>, PojoContainedTypeManager<?>> byExactClass = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void add(PojoRawTypeModel<E> typeModel, PojoContainedTypeManager<E> typeManager) {
			byExactClass.put( typeModel.getJavaClass(), typeManager );
		}

		public void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.pushAll( PojoContainedTypeManager::close, byExactClass.values() );
			}
		}

		public PojoContainedTypeManagerContainer build() {
			return new PojoContainedTypeManagerContainer( this );
		}
	}

}
