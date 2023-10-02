/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial and final step in named predicate definition.
 */
@Incubating
public interface NamedPredicateOptionsStep extends PredicateFinalStep {

	NamedPredicateOptionsStep param(String name, Object value);

}
