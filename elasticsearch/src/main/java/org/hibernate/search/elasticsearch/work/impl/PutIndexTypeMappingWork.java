/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;

import com.google.gson.Gson;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.mapping.PutMapping;

/**
 * @author Yoann Rodiere
 */
public class PutIndexTypeMappingWork extends SimpleElasticsearchWork<JestResult> {

	protected PutIndexTypeMappingWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final PutMapping.Builder jestBuilder;

		public Builder(
				GsonService gsonService,
				String indexName, String typeName, TypeMapping typeMapping) {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonService.getGsonNoSerializeNulls();
			String serializedMapping = gson.toJson( typeMapping );
			this.jestBuilder = new PutMapping.Builder( indexName, typeName, serializedMapping );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public PutIndexTypeMappingWork build() {
			return new PutIndexTypeMappingWork( this );
		}
	}
}