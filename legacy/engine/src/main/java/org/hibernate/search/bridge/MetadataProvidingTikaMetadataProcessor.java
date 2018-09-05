/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.hibernate.search.bridge.spi.FieldMetadataBuilder;

/**
 * Optional contract to be implemented by {@link TikaMetadataProcessor} implementations wishing to expose
 * meta-data related to the fields they create.
 * <p>
 *
 * @see TikaMetadataProcessor
 * @see MetadataProvidingFieldBridge
 * @author Gunnar Morling
 * @author Yoann Rodiere
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing implementations.
 */
public interface MetadataProvidingTikaMetadataProcessor extends TikaMetadataProcessor {

	/**
	 * Allows this processor to expose meta-data about the fields it creates.
	 *
	 * @param name The default field name; Should be used consistently with
	 * {@link FieldBridge#set(String, Object, org.apache.lucene.document.Document, LuceneOptions)}.
	 * @param builder Builder for exposing field-related metadata
	 */
	void configureFieldMetadata(String name, FieldMetadataBuilder builder);
}
