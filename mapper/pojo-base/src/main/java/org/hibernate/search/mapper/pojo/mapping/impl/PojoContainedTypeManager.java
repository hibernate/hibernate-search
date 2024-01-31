/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContext;

/**
 * @param <I> The identifier type for the contained entity type.
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeManager<I, E> extends AbstractPojoTypeManager<I, E>
		implements PojoWorkContainedTypeContext<I, E>, PojoScopeContainedTypeContext<I, E> {
	private PojoContainedTypeManager(Builder<I, E> builder) {
		super( builder );
	}

	@Override
	public Optional<PojoContainedTypeManager<I, E>> asContained() {
		return Optional.of( this );
	}

	public static class Builder<I, E> extends AbstractBuilder<I, E> {
		public Builder(PojoRawTypeModel<E> typeModel, String entityName,
				boolean singleConcreteTypeInEntityHierarchy,
				IdentifierMappingImplementor<I, E> identifierMapping,
				PojoPathOrdinals pathOrdinals,
				PojoImplicitReindexingResolver<E> reindexingResolver) {
			super( typeModel, entityName, singleConcreteTypeInEntityHierarchy, identifierMapping,
					pathOrdinals, reindexingResolver );
		}

		@Override
		public PojoContainedTypeManager<I, E> build() {
			return new PojoContainedTypeManager<>( this );
		}
	}
}
