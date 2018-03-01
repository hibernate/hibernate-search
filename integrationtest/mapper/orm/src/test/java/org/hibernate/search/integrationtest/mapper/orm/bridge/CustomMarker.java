/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bridge;

import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.integrationtest.mapper.orm.bridge.annotation.CustomMarkerAnnotation;

public final class CustomMarker {

	public static class Builder implements AnnotationMarkerBuilder<CustomMarkerAnnotation> {
		@Override
		public void initialize(CustomMarkerAnnotation annotation) {
			// Nothing to do
		}

		@Override
		public Object build() {
			return new CustomMarker();
		}
	}

	private CustomMarker() {
	}

}
