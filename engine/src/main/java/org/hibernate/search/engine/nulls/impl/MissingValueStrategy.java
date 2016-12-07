/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nulls.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.engine.metadata.impl.DocumentFieldPath;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;

/**
 * Strategy for handling missing values.
 *
 * <p>Currently only serves as a factory for {@link NullMarkerCodec}s, but could be
 * extended in the future to also handle exists/missing queries (see HSEARCH-2389).
 *
 * @author Yoann Rodiere
 */
public interface MissingValueStrategy {

	/**
	 * @param entityType The entity on which the null marker is being used.
	 * @param path The path of the field on which the null marker is being used.
	 * @param marker The null marker to use when indexing/querying null values.
	 * @return A codec that will index and query the given marker.
	 */
	NullMarkerCodec createNullMarkerCodec(Class<?> entityType, DocumentFieldPath path, NullMarker marker);

}
