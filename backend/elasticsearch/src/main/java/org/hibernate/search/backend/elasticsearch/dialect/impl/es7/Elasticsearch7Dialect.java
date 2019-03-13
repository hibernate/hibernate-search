/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl.es7;

import org.hibernate.search.backend.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.Elasticsearch7WorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;

import com.google.gson.GsonBuilder;

/**
 * The dialect for Elasticsearch 7.
 */
public class Elasticsearch7Dialect implements ElasticsearchDialect {

	@Override
	public GsonBuilder createGsonBuilderBase() {
		return new GsonBuilder();
	}

	@Override
	public ElasticsearchWorkBuilderFactory createWorkBuilderFactory(GsonProvider gsonProvider) {
		return new Elasticsearch7WorkBuilderFactory( gsonProvider );
	}
}
