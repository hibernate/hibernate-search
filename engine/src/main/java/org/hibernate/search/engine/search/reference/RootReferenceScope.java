/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.mapper.scope.SearchScope;
import org.hibernate.search.engine.mapper.scope.SearchScopeProvider;

public interface RootReferenceScope<SR, T> {
	Class<SR> rootReferenceType();

	<ER extends EntityReference, S extends SearchScope<SR, T, ER>, P extends SearchScopeProvider<ER>> S create(P scopeProvider);
}
