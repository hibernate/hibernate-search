/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface SpatialMatchingContext /** TODO ?extends Fieldcustomization<SpatialMatchingContext> */
{
	WithinContext within(double distance, Unit unit);
}
