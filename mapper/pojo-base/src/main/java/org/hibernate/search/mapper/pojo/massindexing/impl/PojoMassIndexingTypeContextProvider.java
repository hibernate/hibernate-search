/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoMassIndexingTypeContextProvider {

	<E> Optional<? extends PojoMassIndexingIndexedTypeContext<E>> forExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	<E> Optional<? extends Set<? extends PojoMassIndexingIndexedTypeContext<? extends E>>> allForSuperType(
			PojoRawTypeIdentifier<E> typeIdentifier);

}
