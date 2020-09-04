/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope.impl;

import org.hibernate.search.mapper.javabean.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;

/**
 * A mapper-specific indexed type context,
 * accessible through {@link PojoScopeDelegate#includedIndexedTypes()}
 * in particular.
 *
 * @param <E> The entity type mapped to the index.
 */
public interface JavaBeanScopeIndexedTypeContext<E>
		extends SearchIndexedEntity<E> {

}
