/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search;

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
