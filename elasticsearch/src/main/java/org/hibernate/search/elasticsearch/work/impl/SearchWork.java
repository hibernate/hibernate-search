/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.Collection;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchRequestUtils;
import org.hibernate.search.util.logging.impl.LogCategory;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;

/**
 * @author Yoann Rodiere
 */
public class SearchWork extends SimpleElasticsearchWork<SearchResult> {

	private static final Log QUERY_LOG = LoggerFactory.make( Log.class, LogCategory.QUERY );

	protected SearchWork(Builder builder) {
		super( builder );
	}

	@Override
	protected void beforeExecute(ElasticsearchWorkExecutionContext executionContext) {
		if ( QUERY_LOG.isDebugEnabled() ) {
			Search search = (Search) action;
			GsonService gsonService = executionContext.getGsonService();
			QUERY_LOG.executingElasticsearchQuery(
					// We use getURI(), but the name is confusing: it's actually the path + query parts of the URL
					search.getURI(),
					ElasticsearchRequestUtils.formatRequestData( gsonService, search )
					);
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, SearchResult> {
		private final Search.Builder jestBuilder;

		public Builder(String payload) {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Search.Builder( payload );
		}

		public Builder indexes(Collection<String> indexNames) {
			jestBuilder.addIndex( indexNames );
			return this;
		}

		public Builder paging(int firstResult, int size) {
			jestBuilder.setParameter( Parameters.FROM, firstResult );
			jestBuilder.setParameter( Parameters.SIZE, size );
			return this;
		}

		public Builder scrolling(int scrollSize, String scrollTimeout) {
			jestBuilder.setParameter( Parameters.SIZE, scrollSize );
			jestBuilder.setParameter( Parameters.SCROLL, scrollTimeout );
			return this;
		}

		public Builder appendSort(Sort sort) {
			jestBuilder.addSort( sort );
			return this;
		}

		@Override
		protected Search buildAction() {
			return jestBuilder.build();
		}

		@Override
		public ElasticsearchWork<SearchResult> build() {
			return new SearchWork( this );
		}
	}
}
