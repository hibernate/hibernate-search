/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl;


import java.util.Optional;

/**
 * @author Yoann Rodiere
 */
public interface QueryClauseExtension<N, T> {

	T extendOrFail(QueryClauseContainerContext<N> original);

	Optional<T> extendOptional(QueryClauseContainerContext<N> original);

}
