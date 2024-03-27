/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public interface ProjectionRequestContext {

	void checkValidField(String absoluteFieldPath);

	void checkNotNested(SearchQueryElementTypeKey<?> projectionKey, String hint);

	ProjectionRequestRootContext root();

	ProjectionRequestContext forField(String absoluteFieldPath, String[] absoluteFieldPathComponents);

	String absoluteCurrentFieldPath();

	String[] relativeCurrentFieldPathComponents();

}
