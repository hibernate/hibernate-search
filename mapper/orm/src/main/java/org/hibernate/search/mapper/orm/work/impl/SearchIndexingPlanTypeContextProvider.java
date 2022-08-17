/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface SearchIndexingPlanTypeContextProvider {

	<T> PojoTypeContext<T> forExactClass(Class<T> javaClass);

	KeyValueProvider<String, ? extends PojoTypeContext<?>> byEntityName();

}
