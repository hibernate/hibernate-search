/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

public interface IndexObjectFieldBuilder extends IndexCompositeNodeBuilder {

	/**
	 * Mark the current node as multi-valued.
	 * <p>
	 * This informs the backend that this field may contain multiple objects for a single document.
	 */
	void multiValued();

	IndexObjectFieldReference toReference();
}
