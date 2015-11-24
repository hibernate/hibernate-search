/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

/**
 * Allows to configure field-related meta-data.
 *
 * @author Gunnar Morling
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing users.
 */
public interface FieldMetadataCreationContext {

	/**
	 * Adds a field to the list of all meta-data.
	 *
	 * @param name The name of the field
	 * @param type The type of the field; The type of the field created in
	 * {@link org.hibernate.search.bridge.FieldBridge#set(String, Object, org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)}
	 * must match the type declared for the field here.
	 * @return a context object for fluent API invocations
	 */
	FieldMetadataCreationContext field(String name, FieldType type);

	/**
	 * Marks the last added field as sortable.
	 *
	 * @param sortable whether the last added field is sortable or not.
	 * @return this context for fluent API invocations
	 */
	FieldMetadataCreationContext sortable(boolean sortable);
}
