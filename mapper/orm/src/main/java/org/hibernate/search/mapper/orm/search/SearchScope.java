/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search;

/**
 * @param <E> A supertype of all types in this scope.
 * @deprecated Use {@link org.hibernate.search.mapper.orm.scope.SearchScope} instead.
 */
@Deprecated
public interface SearchScope<E> extends org.hibernate.search.mapper.orm.scope.SearchScope<E> {

}
