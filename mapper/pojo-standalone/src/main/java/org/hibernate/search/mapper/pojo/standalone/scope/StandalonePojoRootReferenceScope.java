/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.reference.RootReferenceScope;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface StandalonePojoRootReferenceScope<SR, T> extends RootReferenceScope<SR, T> {

	TypedSearchScope<SR, T> scope(SearchScopeProvider scopeProvider);

	default SearchQuerySelectStep<SR, ?, EntityReference, T, ?, ?, ?> search(SearchSession session) {
		return session.search( scope( session ) );
	}
}
