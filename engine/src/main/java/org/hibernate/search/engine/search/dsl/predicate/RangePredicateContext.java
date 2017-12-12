/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * @author Yoann Rodiere
 */
public interface RangePredicateContext<N> {

	default RangePredicateFieldSetContext<N> onField(String fieldName) {
		return onFields( fieldName );
	}

	RangePredicateFieldSetContext<N> onFields(String ... fieldName);

}
