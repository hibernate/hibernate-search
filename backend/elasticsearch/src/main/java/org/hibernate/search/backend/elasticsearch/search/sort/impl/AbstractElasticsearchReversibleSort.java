/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class AbstractElasticsearchReversibleSort extends AbstractElasticsearchSort {

	private static final JsonAccessor<JsonElement> ORDER_ACCESSOR = JsonAccessor.root().property( "order" );
	private static final JsonPrimitive ASC_KEYWORD_JSON = new JsonPrimitive( "asc" );
	private static final JsonPrimitive DESC_KEYWORD_JSON = new JsonPrimitive( "desc" );

	private final SortOrder order;

	protected AbstractElasticsearchReversibleSort(AbstractBuilder builder) {
		super( builder );
		order = builder.order;
	}

	@Override
	public final void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		JsonObject innerObject = new JsonObject();

		if ( order != null ) {
			switch ( order ) {
				case ASC:
					ORDER_ACCESSOR.set( innerObject, ASC_KEYWORD_JSON );
					break;
				case DESC:
					ORDER_ACCESSOR.set( innerObject, DESC_KEYWORD_JSON );
					break;
			}
		}
		enrichInnerObject( collector, innerObject );
		doToJsonSorts( collector, innerObject );
	}

	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
	}

	protected abstract void doToJsonSorts(ElasticsearchSearchSortCollector collector, JsonObject innerObject);

	public abstract static class AbstractBuilder extends AbstractElasticsearchSort.AbstractBuilder {

		protected SortOrder order;

		protected AbstractBuilder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		public void order(SortOrder order) {
			this.order = order;
		}

	}
}
