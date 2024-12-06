/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;

public interface SortRequestContext {

	PredicateRequestContext toPredicateRequestContext(String absoluteNestedPath);

}
