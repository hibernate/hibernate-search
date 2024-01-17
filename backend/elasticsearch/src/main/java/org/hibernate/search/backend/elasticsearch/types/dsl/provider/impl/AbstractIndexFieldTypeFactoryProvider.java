/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchIndexFieldTypeFactoryImpl;
import org.hibernate.search.backend.elasticsearch.types.format.impl.Elasticsearch7DefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

abstract class AbstractIndexFieldTypeFactoryProvider
		implements ElasticsearchIndexFieldTypeFactoryProvider {

	private final Gson userFacingGson;
	private final Elasticsearch7DefaultFieldFormatProvider defaultFieldFormatProvider =
			new Elasticsearch7DefaultFieldFormatProvider();

	public AbstractIndexFieldTypeFactoryProvider(Gson userFacingGson) {
		this.userFacingGson = userFacingGson;
	}

	@Override
	public final ElasticsearchIndexFieldTypeFactory create(EventContext eventContext,
			BackendMapperContext backendMapperContext, IndexFieldTypeDefaultsProvider typeDefaultsProvider) {
		return new ElasticsearchIndexFieldTypeFactoryImpl(
				eventContext, backendMapperContext, userFacingGson, defaultFieldFormatProvider, typeDefaultsProvider,
				vectorFieldTypeMappingContributor()
		);
	}

	protected abstract ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor();
}
