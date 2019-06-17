/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public interface PojoScopeIndexedTypeContext<I, E, D extends DocumentElement>
		extends PojoWorkIndexedTypeContext<I, E, D> {

	PojoMappingTypeMetadata getMappingMetadata();

	<R, E2> MappedIndexScopeBuilder<R, E2> createScopeBuilder(MappingContextImplementor mappingContext);

	void addTo(MappedIndexScopeBuilder<?, ?> builder);

}
