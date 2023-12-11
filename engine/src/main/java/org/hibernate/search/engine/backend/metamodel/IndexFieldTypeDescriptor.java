/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Set;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;

/**
 * The type of a field in the index,
 * exposing its various capabilities.
 *
 * @see IndexFieldDescriptor#type()
 * @see IndexValueFieldTypeDescriptor
 * @see IndexObjectFieldTypeDescriptor
 */
public interface IndexFieldTypeDescriptor {

	/**
	 * @return An (unmodifiable) set of strings
	 * representing the {@link IndexFieldTraits field traits}
	 * enabled for fields of this type.
	 */
	Set<String> traits();

}
