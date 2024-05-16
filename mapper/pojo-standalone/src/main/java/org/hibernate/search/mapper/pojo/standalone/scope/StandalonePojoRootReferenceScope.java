/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.scope;

import org.hibernate.search.engine.search.reference.RootReferenceScope;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface StandalonePojoRootReferenceScope<SR, T> extends RootReferenceScope<SR, T> {

	SearchScope<SR, T> create(SearchScopeProvider scopeProvider);
}
