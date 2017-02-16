/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.SearchScroll;

/**
 * @author Yoann Rodiere
 */
public class ScrollWork extends SimpleElasticsearchWork<JestResult, SearchResult> {

	protected ScrollWork(Builder builder) {
		super( builder );
	}

	@Override
	protected SearchResult generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return new SearchWork.SearchResultImpl( response.getJsonObject() );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final SearchScroll.Builder jestBuilder;

		public Builder(String scrollId, String scrollTimeout) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new SearchScroll.Builder( scrollId, scrollTimeout );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public ScrollWork build() {
			return new ScrollWork( this );
		}
	}
}