/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.work.impl.builder.ExplainWorkBuilder;

import com.google.gson.JsonObject;

import io.searchbox.action.Action;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Explain;

/**
 * @author Yoann Rodiere
 */
public class ExplainWork extends SimpleElasticsearchWork<DocumentResult, ExplainResult> {

	protected ExplainWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, DocumentResult>
			implements ExplainWorkBuilder {
		private final Explain.Builder jestBuilder;

		public Builder(String indexName, String typeName, String id, JsonObject payload) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Explain.Builder( indexName, typeName, id, payload );
		}

		@Override
		protected Action<DocumentResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public ExplainWork build() {
			return new ExplainWork( this );
		}
	}

	@Override
	protected ExplainResult generateResult(ElasticsearchWorkExecutionContext context, DocumentResult response) {
		return new ExplainResultImpl( response.getJsonObject() );
	}

	private static class ExplainResultImpl implements ExplainResult {

		private final JsonObject jsonObject;

		public ExplainResultImpl(JsonObject jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public JsonObject getJsonObject() {
			return jsonObject;
		}
	}
}