/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
