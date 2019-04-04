/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.Elasticsearch60WorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;

/**
 * The dialect for Elasticsearch 6.0 to 6.6.
 */
public class Elasticsearch60Dialect extends Elasticsearch67Dialect implements ElasticsearchDialect {

	@Override
	public ElasticsearchWorkBuilderFactory createWorkBuilderFactory(GsonProvider gsonProvider) {
		// Necessary because of the breaking changes related to type names in ES6.7/ES7
		return new Elasticsearch60WorkBuilderFactory( gsonProvider );
	}

}
