/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.spatial.GeoPoint;

public class ProjectionExtractContext {

	private final ProjectionRequestRootContext requestContext;

	public ProjectionExtractContext(ProjectionRequestRootContext requestContext) {
		this.requestContext = requestContext;
	}

	Integer getDistanceSortIndex(String absoluteFieldPath, GeoPoint location) {
		return requestContext.getDistanceSortIndex( absoluteFieldPath, location );
	}

}
