/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step of all spatial predicate definitions.
 */
public interface SpatialPredicateInitialStep {

	/**
	 * Match documents where targeted fields point to a location within given bounds:
	 * a circle (maximum distance matching), a polygon, a bounding box, ...
	 *
	 * @return The initial step of a DSL allowing the definition of a "within" predicate.
	 */
	SpatialWithinPredicateFieldStep<?> within();

}
