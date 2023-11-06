/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * TODO: vector : docs
 *
 * @param <F> The type of field values.
 */
@Incubating
public interface VectorFieldTypeOptionsStep<S extends VectorFieldTypeOptionsStep<?, F>, F>
		extends SearchableProjectableIndexFieldTypeOptionsStep<S, F> {

	S dimension(int dimension);

	S vectorSimilarity(VectorSimilarity vectorSimilarity);

	S beamWidth(int beamWidth);

	S maxConnections(int maxConnections);

}
