/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public interface PojoMappingCollectorValueNode extends PojoMappingCollector {

	void valueBinder(ValueBinder binder,
			String relativeFieldName, FieldModelContributor fieldModelContributor);

	void indexedEmbedded(PojoRawTypeModel<?> definingTypeModel, String relativePrefix, ObjectStructure structure,
			Integer maxDepth, Set<String> includePaths, Class<?> targetType);

}
