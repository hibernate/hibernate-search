/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexValueField;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public final class ElasticsearchIndexValueField<F>
		extends AbstractIndexValueField<
				ElasticsearchIndexValueField<F>,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchIndexValueFieldType<F>,
				ElasticsearchIndexCompositeNode,
				F>
		implements ElasticsearchIndexField, ElasticsearchSearchIndexValueFieldContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchIndexValueField(ElasticsearchIndexCompositeNode parent, String relativeFieldName,
			ElasticsearchIndexValueFieldType<F> type, TreeNodeInclusion inclusion, boolean multiValued) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
	}

	@Override
	protected ElasticsearchIndexValueField<F> self() {
		return this;
	}

	@Override
	public ElasticsearchIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchIndexValueField<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.valueClass(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (ElasticsearchIndexValueField<? super T>) this;
	}
}
