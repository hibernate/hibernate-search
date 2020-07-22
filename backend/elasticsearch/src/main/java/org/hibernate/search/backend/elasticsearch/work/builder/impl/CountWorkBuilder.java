/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;

import com.google.gson.JsonObject;

public interface CountWorkBuilder extends ElasticsearchWorkBuilder<NonBulkableWork<Long>> {

	CountWorkBuilder index(URLEncodedString indexName);

	CountWorkBuilder query(JsonObject query);

	CountWorkBuilder routingKeys(Set<String> routingKeys);

	CountWorkBuilder requestTransformer(Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer);

	CountWorkBuilder timeout(Long timeoutValue, TimeUnit timeoutUnit, boolean exceptionOnTimeout);

}
