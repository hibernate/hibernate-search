package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public abstract class AbstractElasticsearchReversibleSort extends AbstractElasticsearchSort {

	private static final JsonAccessor<JsonNode> ORDER_ACCESSOR = JsonAccessor.root().property( "order" );
	private static final JsonNode ASC_KEYWORD_JSON = JsonScalarConversions.toNode( "asc" );
	private static final JsonNode DESC_KEYWORD_JSON = JsonScalarConversions.toNode( "desc" );

	private final SortOrder order;

	protected AbstractElasticsearchReversibleSort(AbstractBuilder builder) {
		super( builder );
		order = builder.order;
	}

	@Override
	public final void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		ObjectNode innerObject = JsonNodeFactory.instance.objectNode();

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

	protected void enrichInnerObject(ElasticsearchSearchSortCollector collector, ObjectNode innerObject) {
	}

	protected abstract void doToJsonSorts(ElasticsearchSearchSortCollector collector, ObjectNode innerObject);

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
