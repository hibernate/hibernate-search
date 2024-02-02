/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoLoadingTypeContext<E> {

	String entityName();

	String secondaryEntityName();

	PojoRawTypeIdentifier<E> typeIdentifier();

	List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes();

	boolean isSingleConcreteTypeInEntityHierarchy();

	PojoSelectionLoadingStrategy<? super E> selectionLoadingStrategy();

	Optional<PojoSelectionLoadingStrategy<? super E>> selectionLoadingStrategyOptional();

	PojoMassLoadingStrategy<? super E, ?> massLoadingStrategy();

	Optional<PojoMassLoadingStrategy<? super E, ?>> massLoadingStrategyOptional();

}
