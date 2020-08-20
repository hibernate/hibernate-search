/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nulls.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;
import org.hibernate.search.spi.IndexedTypeIdentifier;

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
	 * @param fieldMetadata The field metadata we know about so far.
	 * @param marker The null marker to use when indexing/querying null values.
	 * @return A codec that will index and query the given marker.
	 */
	NullMarkerCodec createNullMarkerCodec(IndexedTypeIdentifier entityType,
			PartialDocumentFieldMetadata fieldMetadata, NullMarker marker);

}
