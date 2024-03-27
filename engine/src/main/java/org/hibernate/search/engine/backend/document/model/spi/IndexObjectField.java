/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexObjectField<
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexCompositeNodeType<SC, ?>,
		C extends IndexCompositeNode<SC, NT, F>,
		F extends IndexField<SC, ?>>
		extends IndexNode<SC>, IndexField<SC, C>, IndexCompositeNode<SC, NT, F>,
		IndexObjectFieldDescriptor {

}
