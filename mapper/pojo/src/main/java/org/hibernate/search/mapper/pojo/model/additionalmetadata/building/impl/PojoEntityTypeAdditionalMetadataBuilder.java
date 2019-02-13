/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorEntityTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class PojoEntityTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorEntityTypeNode {
	private final PojoTypeAdditionalMetadataBuilder rootBuilder;
	private final PojoPathFilterFactory<Set<String>> pathFilterFactory;
	private String entityIdPropertyName;

	PojoEntityTypeAdditionalMetadataBuilder(PojoTypeAdditionalMetadataBuilder rootBuilder,
			PojoPathFilterFactory<Set<String>> pathFilterFactory) {
		this.rootBuilder = rootBuilder;
		this.pathFilterFactory = pathFilterFactory;
	}

	@Override
	public ContextualFailureCollector getFailureCollector() {
		// There's nothing to add to the context
		return rootBuilder.getFailureCollector();
	}

	@Override
	public void entityIdPropertyName(String propertyName) {
		this.entityIdPropertyName = propertyName;
	}

	public PojoEntityTypeAdditionalMetadata build() {
		return new PojoEntityTypeAdditionalMetadata( pathFilterFactory, Optional.ofNullable( entityIdPropertyName ) );
	}
}
