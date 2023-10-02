/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in "not" predicate definition.
 */
public interface NotPredicateFinalStep extends PredicateFinalStep, PredicateScoreStep<NotPredicateFinalStep> {
}
