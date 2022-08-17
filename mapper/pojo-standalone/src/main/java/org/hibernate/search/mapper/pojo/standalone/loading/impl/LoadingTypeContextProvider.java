/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface LoadingTypeContextProvider {

	<E> LoadingTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier);

	<E> LoadingTypeContext<E> forExactTypeOrNull(PojoRawTypeIdentifier<E> typeIdentifier);

	<E> LoadingTypeContext<E> indexedForExactClass(Class<E> typeIdentifier);

}
