/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldContributor;

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
	 * @return A field contributor for the additional implicit properties (_entity_type, ...),
	 * or an empty optional.
	 */
	Optional<ImplicitFieldContributor> getImplicitFieldContributor();

	/**
	 * Register a new index => type mapping.
	 *
	 * @param indexNames The names of the index.
	 * @param mappedTypeName The name of the type mapped to the index.
	 */
	void register(IndexNames indexNames, String mappedTypeName);

	/**
	 * @return A helper for projections that need to extract the mapped type name from search hits.
	 */
	ProjectionExtractionHelper<String> getTypeNameExtractionHelper();

}
