/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.mapper.scope.SearchScope;
import org.hibernate.search.engine.mapper.scope.SearchScopeProvider;

public interface RootReferenceScope<SR, T> {
	Class<SR> rootReferenceType();

	<ER extends EntityReference, S extends SearchScope<SR, T, ER>, P extends SearchScopeProvider<ER>> S create(P scopeProvider);
}
