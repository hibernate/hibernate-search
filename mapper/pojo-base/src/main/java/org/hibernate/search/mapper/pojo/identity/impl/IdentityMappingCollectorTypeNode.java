/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

class IdentityMappingCollectorTypeNode<T> extends AbstractIdentityMappingCollectorNode
		implements PojoMappingCollectorTypeNode {

	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final PojoIdentityMappingCollector identityMappingCollector;

	IdentityMappingCollectorTypeNode(BoundPojoModelPathTypeNode<T> modelPath,
			PojoMappingHelper mappingHelper,
			PojoIdentityMappingCollector identityMappingCollector) {
		super( mappingHelper );
		this.modelPath = modelPath;
		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPath() {
		return modelPath;
	}

	@Override
	public void typeBinder(TypeBinder builder, Map<String, Object> params) {
		// No-op, we're just collecting the identity mapping.
	}

	@Override
	public PojoMappingCollectorPropertyNode property(String propertyName) {
		return new IdentityMappingCollectorPropertyNode<>( modelPath.property( propertyName ),
				mappingHelper, identityMappingCollector
		);
	}

}
