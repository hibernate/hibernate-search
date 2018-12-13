/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.spatial.GeoPoint;

public class SearchProjectionExecutionContext {

	private final FromDocumentFieldValueConvertContext fromDocumentFieldValueConvertContext;
	private final Map<DistanceSortKey, Integer> distanceSorts;

	public SearchProjectionExecutionContext(SessionContextImplementor sessionContext,
			Map<DistanceSortKey, Integer> distanceSorts) {
		this.fromDocumentFieldValueConvertContext = new FromDocumentFieldValueConvertContextImpl( sessionContext );
		this.distanceSorts = distanceSorts != null ? Collections.unmodifiableMap( distanceSorts ) : null;
	}

	FromDocumentFieldValueConvertContext getFromDocumentFieldValueConvertContext() {
		return fromDocumentFieldValueConvertContext;
	}

	Integer getDistanceSortIndex(String absoluteFieldPath, GeoPoint location) {
		if ( distanceSorts == null ) {
			return null;
		}

		return distanceSorts.get( new DistanceSortKey( absoluteFieldPath, location ) );
	}

	public static class DistanceSortKey {

		private final String absoluteFieldPath;

		private final GeoPoint location;

		public DistanceSortKey(String absoluteFieldPath, GeoPoint location) {
			this.absoluteFieldPath = absoluteFieldPath;
			this.location = location;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( !(obj instanceof DistanceSortKey) ) {
				return false;
			}

			DistanceSortKey other = (DistanceSortKey) obj;

			return Objects.equals( this.absoluteFieldPath, other.absoluteFieldPath )
					&& Objects.equals( this.location, other.location );
		}

		@Override
		public int hashCode() {
			return Objects.hash( absoluteFieldPath, location );
		}
	}
}
