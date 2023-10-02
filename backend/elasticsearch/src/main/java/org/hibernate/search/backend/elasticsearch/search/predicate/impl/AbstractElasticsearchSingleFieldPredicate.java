/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

public abstract class AbstractElasticsearchSingleFieldPredicate extends AbstractElasticsearchNestablePredicate {

	protected final String absoluteFieldPath;
	private final List<String> nestedPathHierarchy;

	protected AbstractElasticsearchSingleFieldPredicate(AbstractBuilder builder) {
		super( builder );
		absoluteFieldPath = builder.absoluteFieldPath;
		nestedPathHierarchy = builder.nestedPathHierarchy;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return Collections.singletonList( absoluteFieldPath );
	}

	protected abstract static class AbstractBuilder extends AbstractElasticsearchPredicate.AbstractBuilder {
		protected final String absoluteFieldPath;
		private final List<String> nestedPathHierarchy;

		protected AbstractBuilder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexNodeContext node) {
			this( scope, node.absolutePath(), node.nestedPathHierarchy() );
		}

		protected AbstractBuilder(ElasticsearchSearchIndexScope<?> scope, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			super( scope );
			this.absoluteFieldPath = absoluteFieldPath;
			this.nestedPathHierarchy = nestedPathHierarchy;
		}
	}

}
