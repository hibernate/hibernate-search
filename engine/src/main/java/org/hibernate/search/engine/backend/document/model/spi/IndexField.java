/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexField<
		SC extends SearchIndexScope<?>,
		C extends IndexCompositeNode<SC, ?, ?>>
		extends IndexNode<SC>, IndexFieldDescriptor {

	@Override
	C toComposite();

	@Override
	IndexObjectField<SC, ?, C, ?> toObjectField();

	@Override
	IndexValueField<SC, ?, C> toValueField();

	@Override
	C parent();

}
