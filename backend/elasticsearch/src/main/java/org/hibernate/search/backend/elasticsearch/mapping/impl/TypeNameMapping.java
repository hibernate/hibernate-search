/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;

/**
 * Regroups behavior related to how type names are assigned to index documents.
 */
public interface TypeNameMapping {

	/**
	 * @return A schema contributor for the required additional properties (type name, ...),
	 * or an empty optional.
	 */
	Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor();

	/**
	 * @param mappedTypeName The name of the type mapped to the index.
	 * @return A document metadata contributor for the required additional properties (type name, ...),
	 * or an empty optional.
	 */
	Optional<DocumentMetadataContributor> getDocumentMetadataContributor(String mappedTypeName);

	/**
	 * Register a new index => type mapping.
	 *
	 * @param elasticsearchIndexName The name of the index as returned by Elasticsearch.
	 * @param mappedTypeName The name of the type mapped to the index.
	 */
	void register(String elasticsearchIndexName, String mappedTypeName);

	/**
	 * @return A helper for projections that need to extract the mapped type name from search hits.
	 */
	ProjectionExtractionHelper<String> getTypeNameExtractionHelper();

}
