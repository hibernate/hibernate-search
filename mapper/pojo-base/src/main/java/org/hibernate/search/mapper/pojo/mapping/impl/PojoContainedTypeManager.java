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
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContext;

/**
 * @param <I> The identifier type for the contained entity type.
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeManager<I, E> extends AbstractPojoTypeManager<I, E>
		implements PojoWorkContainedTypeContext<I, E>, PojoScopeContainedTypeContext<I, E> {
	public PojoContainedTypeManager(String entityName, PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster, boolean singleConcreteTypeInEntityHierarchy,
			IdentifierMappingImplementor<I, E> identifierMapping, PojoPathOrdinals pathOrdinals,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		super( entityName, typeIdentifier, caster, singleConcreteTypeInEntityHierarchy,
				identifierMapping, pathOrdinals, reindexingResolver );
	}

	@Override
	public Optional<PojoContainedTypeManager<I, E>> asContained() {
		return Optional.of( this );
	}
}
