/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeTypeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchNestedPredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchObjectProjection;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class ElasticsearchIndexCompositeNodeType
		extends AbstractIndexCompositeNodeType<
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchSearchIndexCompositeNodeContext>
		implements ElasticsearchSearchIndexCompositeNodeTypeContext {

	private ElasticsearchIndexCompositeNodeType(Builder builder) {
		super( builder );
	}

	public PropertyMapping createMapping(DynamicType dynamicType) {
		PropertyMapping mapping = new PropertyMapping();
		mapping.setType( nested() ? DataTypes.NESTED : DataTypes.OBJECT );
		mapping.setDynamic( dynamicType );
		return mapping;
	}

	public static class Builder
			extends AbstractIndexCompositeNodeType.Builder<
					ElasticsearchSearchIndexScope<?>,
					ElasticsearchSearchIndexCompositeNodeContext> {
		public Builder(ObjectStructure objectStructure) {
			super( objectStructure );
			queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.ObjectFieldFactory() );
			queryElementFactory( ProjectionTypeKeys.OBJECT, new ElasticsearchObjectProjection.Factory() );
			if ( ObjectStructure.NESTED.equals( objectStructure ) ) {
				queryElementFactory( PredicateTypeKeys.NESTED, new ElasticsearchNestedPredicate.Factory() );
			}
		}

		@Override
		public ElasticsearchIndexCompositeNodeType build() {
			return new ElasticsearchIndexCompositeNodeType( this );
		}
	}
}
