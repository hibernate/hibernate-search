/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContextProvider;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface PojoScopeTypeContextProvider
		extends PojoWorkTypeContextProvider, PojoMassIndexingTypeContextProvider {

	@Override
	<E> PojoScopeIndexedTypeContext<?, E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	Set<PojoRawTypeIdentifier<?>> allNonInterfaceSuperTypes();

	Set<PojoRawTypeIdentifier<?>> allIndexedAndContainedTypes();

	<E> Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> indexedForSuperTypes(
			Collection<? extends PojoRawTypeIdentifier<? extends E>> typeIdentifiers);

	@Override
	<E> Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> indexedForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier);

	KeyValueProvider<String, PojoRawTypeIdentifier<?>> nonInterfaceSuperTypeIdentifierByEntityName();

	<E> PojoRawTypeIdentifier<E> nonInterfaceSuperTypeIdentifierForClass(Class<E> clazz);

	<E> Set<? extends PojoWorkTypeContext<?, ? extends E>> forNonInterfaceSuperType(PojoRawTypeIdentifier<E> typeIdentifier);

}
