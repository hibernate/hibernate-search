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

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContextProvider;
import org.hibernate.search.util.common.impl.Closer;

public class PojoContainedTypeManagerContainer
		implements PojoWorkContainedTypeContextProvider, PojoScopeContainedTypeContextProvider {

	public static Builder builder() {
		return new Builder();
	}

	private final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeManager<?>> byExactType;
	private final Set<PojoContainedTypeManager<?>> all;

	private PojoContainedTypeManagerContainer(Builder builder) {
		this.byExactType = new HashMap<>( builder.byExactClass );
		this.all = Collections.unmodifiableSet( new LinkedHashSet<>( byExactType.values() ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Optional<? extends PojoContainedTypeManager<E>> getByExactType(
			PojoRawTypeIdentifier<E> typeIdentifier) {
		return Optional.ofNullable( (PojoContainedTypeManager<E>) byExactType.get( typeIdentifier ) );
	}

	Set<PojoContainedTypeManager<?>> getAll() {
		return all;
	}

	public static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeManager<?>> byExactClass = new LinkedHashMap<>();

		private Builder() {
		}

		public <E> void add(PojoRawTypeModel<E> typeModel, PojoContainedTypeManager<E> typeManager) {
			byExactClass.put( typeModel.getTypeIdentifier(), typeManager );
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
