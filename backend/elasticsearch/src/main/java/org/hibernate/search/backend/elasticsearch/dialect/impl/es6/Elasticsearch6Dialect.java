/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl.es6;

import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.Elasticsearch6WorkBuilderFactory;

import com.google.gson.GsonBuilder;

/**
 * The dialect for Elasticsearch 6.
 */
public class Elasticsearch6Dialect implements ElasticsearchDialect {

	@Override
	public GsonBuilder createGsonBuilderBase() {
		// No specific needs for ES5.
		return new GsonBuilder();
	}

	@Override
	public Elasticsearch6WorkBuilderFactory createWorkBuilderFactory(GsonProvider gsonProvider) {
		/*
		 * The ES6 factory works fine with ES5 so far.
		 * We may have to override some methods in the future if we add new work types, though.
		 */
		return new Elasticsearch6WorkBuilderFactory( gsonProvider );
	}
}
