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
public class ScrollWork extends SimpleElasticsearchWork<JestResult> {

	protected ScrollWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final SearchScroll.Builder jestBuilder;

		public Builder(String scrollId, String scrollTimeout) {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
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