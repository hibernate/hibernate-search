/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoWorkIndexedTypeContextProvider {

	<E> Optional<? extends PojoWorkIndexedTypeContext<?, E>> getByExactType(PojoRawTypeIdentifier<E> typeIdentifier);

}
