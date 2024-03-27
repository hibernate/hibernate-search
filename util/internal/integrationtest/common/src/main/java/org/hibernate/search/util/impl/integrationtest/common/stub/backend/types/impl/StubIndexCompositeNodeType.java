/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexCompositeNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexCompositeNodeTypeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubObjectProjection;

public class StubIndexCompositeNodeType
		extends AbstractIndexCompositeNodeType<
				StubSearchIndexScope,
				StubSearchIndexCompositeNodeContext>
		implements StubSearchIndexCompositeNodeTypeContext {

	public final ObjectStructure objectStructure;

	private StubIndexCompositeNodeType(Builder builder) {
		super( builder );
		this.objectStructure = builder.objectStructure;
	}

	public void apply(StubIndexSchemaDataNode.Builder builder) {
		if ( objectStructure != ObjectStructure.DEFAULT ) {
			builder.objectStructure( objectStructure );
		}
	}

	public static class Builder
			extends AbstractIndexCompositeNodeType.Builder<
					StubSearchIndexScope,
					StubSearchIndexCompositeNodeContext> {
		private final ObjectStructure objectStructure;

		public Builder(ObjectStructure objectStructure) {
			super( objectStructure );
			this.objectStructure = objectStructure;
			stubFactories(
					new StubSearchPredicate.Factory(),
					PredicateTypeKeys.NESTED,
					PredicateTypeKeys.EXISTS
			);
			queryElementFactory( ProjectionTypeKeys.OBJECT, new StubObjectProjection.Factory() );
		}

		@SafeVarargs
		private final <T> void stubFactories(AbstractStubSearchQueryElementFactory<T> factory,
				SearchQueryElementTypeKey<? super T>... keys) {
			for ( SearchQueryElementTypeKey<? super T> key : keys ) {
				queryElementFactory( key, factory );
			}
		}

		@Override
		public StubIndexCompositeNodeType build() {
			return new StubIndexCompositeNodeType( this );
		}
	}
}
