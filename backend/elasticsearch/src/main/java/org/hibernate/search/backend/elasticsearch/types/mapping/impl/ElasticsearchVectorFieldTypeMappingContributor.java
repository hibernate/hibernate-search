/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

public interface ElasticsearchVectorFieldTypeMappingContributor {

	void contribute(PropertyMapping mapping, Context context);

	interface Context {
		String type();

		VectorSimilarity vectorSimilarity();

		int dimension();

		Integer beamWidth();

		Integer maxConnections();
	}
}
