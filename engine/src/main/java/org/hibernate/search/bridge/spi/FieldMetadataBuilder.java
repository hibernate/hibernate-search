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
public interface FieldMetadataBuilder {

	/**
	 * Adds a field to the list of all meta-data.
	 *
	 * @param name The name of the field
	 * @param type The type of the field
	 * @return a context object for fluent API invocations
	 */
	FieldMetadataCreationContext field(String name, FieldType type);
}
