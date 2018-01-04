/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * A superinterface for contexts allowing to define multi-field predicates, after at least one field was mentioned.
 *
 * @param <S> The "self" type (the actual type of this context)
 */
public interface MultiFieldPredicateFieldSetContext<S> extends SearchPredicateContext<S> {

	default S orField(String field) {
		return orFields( field );
	}

	S orFields(String ... field);

}
