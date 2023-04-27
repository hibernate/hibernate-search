/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.mapper.orm.loading.impl.LoadingIndexedTypeContextProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospectorTypeContextProvider;
import org.hibernate.search.mapper.orm.work.impl.SearchIndexingPlanTypeContextProvider;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface HibernateOrmSessionTypeContextProvider
		extends HibernateOrmRuntimeIntrospectorTypeContextProvider,
		SearchIndexingPlanTypeContextProvider, LoadingIndexedTypeContextProvider {

	KeyValueProvider<String, ? extends HibernateOrmSessionTypeContext<?>> byJpaEntityName();

}
