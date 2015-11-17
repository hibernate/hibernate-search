/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import java.util.Set;

/**
 * Optional contract to be implemented by {@link FieldBridge} implementations wishing to expose meta-data related to the
 * fields they create.
 *
 * @author Gunnar Morling
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing implementations.
 */
public interface MetadataProvidingFieldBridge extends FieldBridge {

	/**
	 * Returns the names of the sortable fields created by this field bridge.
	 *
	 * @param name The default field name; Should be used consistently with
	 * {@link FieldBridge#set(String, Object, org.apache.lucene.document.Document, LuceneOptions)}.
	 * @return The names of the sortable fields created by this field bridge. Never {@code null}.
	 */
	Set<String> getSortableFieldNames(String name);
}
