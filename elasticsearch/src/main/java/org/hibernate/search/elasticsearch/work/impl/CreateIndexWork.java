/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;

import com.google.gson.Gson;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CreateIndex;

/**
 * @author Yoann Rodiere
 */
public class CreateIndexWork extends SimpleElasticsearchWork<JestResult, CreateIndexResult> {

	protected CreateIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected CreateIndexResult generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		int statusCode = response.getResponseCode();
		if ( 200 <= statusCode && statusCode < 300 ) {
			return CreateIndexResult.CREATED;
		}
		else {
			return CreateIndexResult.ALREADY_EXISTS;
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final GsonService gsonService;
		private final CreateIndex.Builder jestBuilder;

		public Builder(GsonService gsonService, String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.gsonService = gsonService;
			this.jestBuilder = new CreateIndex.Builder( indexName );
		}

		public Builder settings(IndexSettings settings) {
			if ( settings != null ) {
				/*
				 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
				 * We better not include the null fields.
				 */
				Gson gson = gsonService.getGsonNoSerializeNulls();
				jestBuilder.settings( gson.toJson( settings ) );
			}
			return this;
		}

		public Builder ignoreExisting() {
			this.resultAssessor = DefaultElasticsearchRequestSuccessAssessor.builder()
					.ignoreErrorTypes( "index_already_exists_exception" )
					.build();
			return this;
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public CreateIndexWork build() {
			return new CreateIndexWork( this );
		}
	}
}