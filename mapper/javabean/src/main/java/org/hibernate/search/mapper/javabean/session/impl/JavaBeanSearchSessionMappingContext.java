/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.impl;

import java.util.Collection;

import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanBackendMappingContext;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;

public interface JavaBeanSearchSessionMappingContext {

	JavaBeanBackendMappingContext getBackendMappingContext();

	SearchScopeImpl createScope(Collection<? extends Class<?>> types);

}
