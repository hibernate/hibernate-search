/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexCompositeNode<
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexCompositeNodeType<SC, ?>,
		F extends IndexField<SC, ?>>
		extends IndexNode<SC>, IndexCompositeElementDescriptor, SearchIndexCompositeNodeContext<SC> {

	@Override
	NT type();

	@Override
	default Collection<F> staticChildren() {
		return staticChildrenByName().values();
	}

	@Override
	Map<String, F> staticChildrenByName();

}
