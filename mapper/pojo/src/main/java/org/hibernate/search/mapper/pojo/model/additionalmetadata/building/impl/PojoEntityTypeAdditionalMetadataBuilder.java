/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorEntityTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;

class PojoEntityTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorEntityTypeNode {
	private final PojoPathFilterFactory pathFilterFactory;
	private String entityIdPropertyName;

	PojoEntityTypeAdditionalMetadataBuilder(PojoPathFilterFactory pathFilterFactory) {
		this.pathFilterFactory = pathFilterFactory;
	}

	@Override
	public void entityIdPropertyName(String propertyName) {
		this.entityIdPropertyName = propertyName;
	}

	public PojoEntityTypeAdditionalMetadata build() {
		return new PojoEntityTypeAdditionalMetadata( pathFilterFactory, Optional.ofNullable( entityIdPropertyName ) );
	}
}
