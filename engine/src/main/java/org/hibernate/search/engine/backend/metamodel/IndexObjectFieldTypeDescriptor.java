/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The type of an "object" field in the index,
 * exposing its various capabilities.
 *
 * @see IndexObjectFieldDescriptor
 */
public interface IndexObjectFieldTypeDescriptor extends IndexFieldTypeDescriptor {

	/**
	 * @return {@code true} if this object field is represented internally as a nested document,
	 * enabling features such as the {@link SearchPredicateFactory#nested(String) nested predicate}.
	 */
	boolean nested();

}
