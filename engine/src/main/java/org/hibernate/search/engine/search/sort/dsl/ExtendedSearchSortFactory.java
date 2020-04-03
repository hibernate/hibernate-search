/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * A base interface for subtypes of {@link SearchSortFactory} allowing to
 * easily override the predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directtly in user code.
 *
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public interface ExtendedSearchSortFactory<PDF extends SearchPredicateFactory>
		extends SearchSortFactory {

	@Override
	FieldSortOptionsStep<?, PDF> field(String absoluteFieldPath);

}
