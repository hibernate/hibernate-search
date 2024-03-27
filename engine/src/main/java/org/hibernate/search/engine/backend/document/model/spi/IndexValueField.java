/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;

public interface IndexValueField<
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexValueFieldType<SC, ?, ?>,
		C extends IndexCompositeNode<SC, ?, ?>>
		extends IndexNode<SC>, IndexField<SC, C>, IndexValueFieldDescriptor,
		SearchIndexValueFieldContext<SC> {

	@Override
	NT type();

}
