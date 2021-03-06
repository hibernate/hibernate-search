/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.impl;

import org.hibernate.search.mapper.javabean.loading.impl.LoadingTypeContextProvider;
import org.hibernate.search.mapper.javabean.work.impl.SearchIndexingPlanTypeContextProvider;

public interface JavaBeanSearchSessionTypeContextProvider
		extends SearchIndexingPlanTypeContextProvider, LoadingTypeContextProvider {

	JavaBeanSessionIndexedTypeContext<?> indexedForEntityName(String indexName);

}
