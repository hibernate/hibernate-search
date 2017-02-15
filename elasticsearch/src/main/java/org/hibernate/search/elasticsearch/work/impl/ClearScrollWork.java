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
public class ClearScrollWork extends SimpleElasticsearchWork<JestResult> {

	protected ClearScrollWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final SearchScroll.Builder jestBuilder;

		public Builder(String scrollId) {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new SearchScroll.Builder( scrollId, "" );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return new ClearScrollAction( jestBuilder );
		}

		@Override
		public ClearScrollWork build() {
			return new ClearScrollWork( this );
		}
	}

	private static class ClearScrollAction extends SearchScroll {
		protected ClearScrollAction(Builder builder) {
			super( builder );
		}

		@Override
		public String getRestMethodName() {
			return "DELETE";
		}
	}
}