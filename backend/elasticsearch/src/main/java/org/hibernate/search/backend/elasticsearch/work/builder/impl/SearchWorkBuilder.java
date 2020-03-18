/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableElasticsearchWork;


public interface SearchWorkBuilder<R> extends ElasticsearchWorkBuilder<NonBulkableElasticsearchWork<R>> {

	SearchWorkBuilder<R> indexes(Collection<URLEncodedString> indexNames);

	SearchWorkBuilder<R> paging(Integer limit, Integer offset);

	SearchWorkBuilder<R> scrolling(int scrollSize, String scrollTimeout);

	SearchWorkBuilder<R> routingKeys(Set<String> routingKeys);

	SearchWorkBuilder<R> requestTransformer(Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer);

	SearchWorkBuilder<R> timeout(Long timeoutValue, TimeUnit timeoutUnit, boolean exceptionOnTimeout);
}
