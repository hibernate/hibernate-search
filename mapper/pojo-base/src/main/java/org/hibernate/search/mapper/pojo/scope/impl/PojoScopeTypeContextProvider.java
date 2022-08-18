/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContextProvider;

public interface PojoScopeTypeContextProvider
		extends PojoWorkTypeContextProvider, PojoMassIndexingTypeContextProvider {

	@Override
	<E> Optional<? extends PojoScopeIndexedTypeContext<?, E>> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	@Override
	<E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> allIndexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier);

}
