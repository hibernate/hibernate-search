/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree.impl;

import java.util.Optional;
import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;

public final class ExcludeAllTreeNestingContext implements TreeNestingContext {

	public static final ExcludeAllTreeNestingContext INSTANCE = new ExcludeAllTreeNestingContext();

	private ExcludeAllTreeNestingContext() {
	}

	@Override
	public <T> T nest(String relativeName, LeafFactory<T> factory) {
		return factory.create( relativeName, TreeNodeInclusion.EXCLUDED );
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factory) {
		return factory.create( relativeName, TreeNodeInclusion.EXCLUDED, INSTANCE );
	}

	@Override
	public <T> T nestUnfiltered(UnfilteredFactory<T> factory) {
		return factory.create( TreeNodeInclusion.EXCLUDED, "" );
	}

	@Override
	public <T> Optional<T> nestComposed(MappingElement mappingElement, String relativePrefix,
			TreeFilterDefinition definition, TreeFilterPathTracker pathTracker, NestedContextBuilder<T> contextBuilder,
			BiFunction<MappingElement, String, SearchException> cyclicRecursionExceptionFactory) {
		return Optional.empty();
	}
}
