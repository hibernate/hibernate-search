/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.Elasticsearch814VectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for ES8.14+.
 */
public class Elasticsearch814IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final Elasticsearch814VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new Elasticsearch814VectorFieldTypeMappingContributor();

	public Elasticsearch814IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
