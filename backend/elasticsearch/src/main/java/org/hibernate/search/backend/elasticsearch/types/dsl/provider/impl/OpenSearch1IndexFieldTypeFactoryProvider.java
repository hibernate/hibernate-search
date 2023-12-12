/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.OpenSearch1VectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for OpenSearch 1.x/2.x.
 */
public class OpenSearch1IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final OpenSearch1VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new OpenSearch1VectorFieldTypeMappingContributor();

	public OpenSearch1IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
