/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchIndexFieldTypeFactoryContextImpl;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public class Elasticsearch6IndexFieldTypeFactoryContextProvider
		implements ElasticsearchIndexFieldTypeFactoryContextProvider {

	private final Gson userFacingGson;

	public Elasticsearch6IndexFieldTypeFactoryContextProvider(Gson userFacingGson) {
		this.userFacingGson = userFacingGson;
	}

	@Override
	public ElasticsearchIndexFieldTypeFactoryContext create(EventContext eventContext) {
		// FIXME use a different implementation for ES6 and ES7
		return new ElasticsearchIndexFieldTypeFactoryContextImpl( eventContext, userFacingGson );
	}
}
