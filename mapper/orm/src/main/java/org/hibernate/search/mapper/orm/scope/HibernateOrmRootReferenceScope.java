/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.reference.RootReferenceScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface HibernateOrmRootReferenceScope<SR, T> extends RootReferenceScope<SR, T> {

	TypedSearchScope<SR, T> scope(SearchScopeProvider scopeProvider);

	@SuppressWarnings("deprecation")
	default SearchQuerySelectStep<SR, ?, org.hibernate.search.mapper.orm.common.EntityReference, T, ?, ?, ?> search(
			SearchSession session) {
		return session.search( scope( session ) );
	}
}
