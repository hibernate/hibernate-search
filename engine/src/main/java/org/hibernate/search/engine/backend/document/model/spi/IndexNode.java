/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexNode<SC extends SearchIndexScope<?>>
		extends SearchIndexNodeContext<SC> {

	@Override
	IndexCompositeNode<SC, ?, ?> toComposite();

	@Override
	IndexObjectField<SC, ?, ?, ?> toObjectField();

	@Override
	IndexValueField<SC, ?, ?> toValueField();

	TreeNodeInclusion inclusion();

}
