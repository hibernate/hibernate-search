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
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContextProvider;

public interface PojoScopeTypeContextProvider
		extends PojoWorkTypeContextProvider, PojoMassIndexingTypeContextProvider {

	@Override
	<E> PojoScopeIndexedTypeContext<?, E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	Set<PojoRawTypeIdentifier<?>> allIndexedSuperTypes();

	Set<PojoRawTypeIdentifier<?>> allNonInterfaceSuperTypes();

	Set<PojoRawTypeIdentifier<?>> allIndexedAndContainedTypes();

	Set<Class<?>> allNonInterfaceSuperTypesClasses();

	@Override
	<E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> allIndexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier);

	<E> Set<? extends PojoWorkTypeContext<?, ? extends E>> allByNonInterfaceSuperType(PojoRawTypeIdentifier<E> typeIdentifier);

}
