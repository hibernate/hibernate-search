/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.Map;

/**
 * Information about a composite index element targeted by search; either the index root or an object field.
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public interface SearchIndexCompositeNodeContext<SC extends SearchIndexScope<?>>
		extends SearchIndexNodeContext<SC> {

	String absolutePath(String relativeFieldName);

	SearchIndexCompositeNodeTypeContext<SC, ?> type();

	Map<String, ? extends SearchIndexNodeContext<SC>> staticChildrenByName();

}
