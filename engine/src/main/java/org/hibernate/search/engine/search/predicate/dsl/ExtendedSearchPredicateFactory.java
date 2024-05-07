/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * A base interface for subtypes of {@link SearchPredicateFactory} allowing to
 * easily override the self type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <S> The self type, i.e. the exposed type of this factory.
 */
public interface ExtendedSearchPredicateFactory<E, S extends ExtendedSearchPredicateFactory<E, ?>>
		extends SearchPredicateFactory<E> {

	@Override
	S withRoot(String objectFieldPath);

}
