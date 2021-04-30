/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope.impl;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanLoadingContext;

public interface JavaBeanScopeMappingContext extends BackendMappingContext {

	DetachedBackendSessionContext detachedBackendSessionContext(String tenantId);

	JavaBeanLoadingContext.Builder loadingContextBuilder(DetachedBackendSessionContext sessionContext);

}
