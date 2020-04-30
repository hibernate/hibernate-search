/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

/**
 * An "object" field in the index, i.e. a field that holds other fields.
 */
public interface IndexObjectFieldDescriptor extends IndexFieldDescriptor, IndexCompositeElementDescriptor {

	/**
	 * @return The type of this field, exposing its various capabilities.
	 * @see IndexObjectFieldTypeDescriptor
	 */
	IndexObjectFieldTypeDescriptor type();

}
