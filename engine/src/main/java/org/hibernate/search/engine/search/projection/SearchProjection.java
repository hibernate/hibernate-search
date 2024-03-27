/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection;

/**
 * A query projection that can be used to obtain particular values of an indexed document.
 * <p>
 * Implementations of this interface are provided to users by Hibernate Search. Users must not try to implement this
 * interface.
 *
 * @param <P> The type of the element returned by the projection.
 */
public interface SearchProjection<P> {
}
