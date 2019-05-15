/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchIndexFieldTypeFactoryContextImpl;
import org.hibernate.search.backend.elasticsearch.types.format.impl.Elasticsearch7DefaultFieldFormatProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public class Elasticsearch7IndexFieldTypeFactoryContextProvider
		implements ElasticsearchIndexFieldTypeFactoryContextProvider {

	private final Gson userFacingGson;
	private final Elasticsearch7DefaultFieldFormatProvider defaultFieldFormatProvider =
			new Elasticsearch7DefaultFieldFormatProvider();

	public Elasticsearch7IndexFieldTypeFactoryContextProvider(Gson userFacingGson) {
		this.userFacingGson = userFacingGson;
	}

	@Override
	public ElasticsearchIndexFieldTypeFactoryContext create(EventContext eventContext, IndexFieldTypeDefaultsProvider typeDefaultsProvider) {
		return new ElasticsearchIndexFieldTypeFactoryContextImpl( eventContext, userFacingGson, defaultFieldFormatProvider, typeDefaultsProvider );
	}
}
