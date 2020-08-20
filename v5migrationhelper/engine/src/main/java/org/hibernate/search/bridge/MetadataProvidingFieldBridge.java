/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.hibernate.search.bridge.spi.FieldMetadataBuilder;

/**
 * Optional contract to be implemented by {@link FieldBridge} implementations wishing to expose meta-data related to the
 * fields they create.
 * <p>
 * Field bridges should implement this contract if they create field(s) with custom names and wish them to mark as
 * sortable. The required doc value fields will be added by the Hibernate Search engine in that case. Otherwise users
 * may not (efficiently) sort on such custom fields.
 * <p>
 *
 * @author Gunnar Morling
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing implementations.
 */
public interface MetadataProvidingFieldBridge extends FieldBridge {

	/**
	 * Allows this bridge to expose meta-data about the fields it creates.
	 *
	 * @param name The default field name; Should be used consistently with
	 * {@link FieldBridge#set(String, Object, org.apache.lucene.document.Document, LuceneOptions)}.
	 * @param builder Builder for exposing field-related meta-data
	 */
	void configureFieldMetadata(String name, FieldMetadataBuilder builder);
}
