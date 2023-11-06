/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * TODO: vector : docs
 */
@Incubating
public interface PropertyMappingVectorFieldOptionsStep extends PropertyMappingStep {

	PropertyMappingVectorFieldOptionsStep projectable(Projectable projectable);

	PropertyMappingVectorFieldOptionsStep searchable(Searchable searchable);

	PropertyMappingVectorFieldOptionsStep vectorSimilarity(VectorSimilarity vectorSimilarity);

	PropertyMappingVectorFieldOptionsStep beamWidth(int beamWidth);

	PropertyMappingVectorFieldOptionsStep maxConnections(int maxConnections);

	PropertyMappingVectorFieldOptionsStep indexNullAs(String indexNullAs);

}
