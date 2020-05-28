/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

public interface PojoAdditionalMetadataCollectorIndexedTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @param backendName The name of the backend where this type should be indexed,
	 * or {@code null} (the default) to target the default backend.
	 */
	void backendName(String backendName);

	/**
	 * @param indexName The name of the backend where this type should be indexed,
	 * or {@code null} (the default) to derive the index name from the entity type.
	 */
	void indexName(String indexName);

}
