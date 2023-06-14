/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Map;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class IdentityMappingCollectorValueNode extends AbstractIdentityMappingCollectorNode
		implements PojoIndexMappingCollectorValueNode {

	private final BoundPojoModelPathValueNode<?, ?, ?> modelPath;

	IdentityMappingCollectorValueNode(BoundPojoModelPathValueNode<?, ?, ?> modelPath, PojoMappingHelper mappingHelper) {
		super( mappingHelper );
		this.modelPath = modelPath;
	}

	@Override
	BoundPojoModelPathValueNode<?, ?, ?> getModelPath() {
		return modelPath;
	}

	@Override
	public void valueBinder(ValueBinder binder, Map<String, Object> params, String relativeFieldName,
			FieldModelContributor fieldModelContributor) {
		// No-op, we're just collecting the identity mapping.
	}

	@Override
	public void indexedEmbedded(PojoRawTypeIdentifier<?> definingType, String relativePrefix,
			ObjectStructure structure,
			TreeFilterDefinition filterDefinition, boolean includeEmbeddedObjectId,
			Class<?> targetType) {
		// No-op, we're just collecting the identity mapping.
	}
}
