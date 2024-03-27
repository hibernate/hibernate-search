/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.Optional;
import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeContributionListener;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;

public final class NotifyingTreeNestingContext implements TreeNestingContext {
	private final TreeNestingContext delegate;
	private final TreeContributionListener listener;

	public NotifyingTreeNestingContext(TreeNestingContext delegate, TreeContributionListener listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public <T> T nest(String relativeName, LeafFactory<T> factory) {
		return delegate.nest(
				relativeName,
				(prefixedName, inclusion) -> {
					if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onNodeContributed();
					}
					return factory.create( prefixedName, inclusion );
				}
		);
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factory) {
		return delegate.nest(
				relativeName,
				(prefixedName, inclusion, nestedNestingContext) -> {
					if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onNodeContributed();
					}
					// No need to wrap the nested context:
					// if we're included, the listener was notified;
					// if we're excluded, children will be excluded as well.
					return factory.create( prefixedName, inclusion, nestedNestingContext );
				}
		);
	}

	@Override
	public <T> T nestUnfiltered(UnfilteredFactory<T> factory) {
		return delegate.nestUnfiltered(
				(inclusion, prefix) -> {
					if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
						listener.onNodeContributed();
					}
					return factory.create( inclusion, prefix );
				}
		);
	}

	@Override
	public <T> Optional<T> nestComposed(MappingElement mappingElement, String relativePrefix,
			TreeFilterDefinition definition, TreeFilterPathTracker pathTracker, NestedContextBuilder<T> contextBuilder,
			BiFunction<MappingElement, String, SearchException> cyclicRecursionExceptionFactory) {
		Optional<T> result = delegate.nestComposed( mappingElement, relativePrefix, definition, pathTracker,
				contextBuilder, cyclicRecursionExceptionFactory );
		if ( result.isPresent() ) {
			listener.onNodeContributed();
		}
		// No need to wrap the nested context:
		// if we're included, the listener was notified;
		// if we're excluded, children will be excluded as well.
		return result;
	}
}
