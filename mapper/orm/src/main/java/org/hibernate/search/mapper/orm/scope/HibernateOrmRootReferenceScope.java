/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope;

import org.hibernate.search.engine.search.reference.RootReferenceScope;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface HibernateOrmRootReferenceScope<SR, T> extends RootReferenceScope<SR, T> {

	SearchScope<SR, T> scope(SearchScopeProvider scopeProvider);
}
