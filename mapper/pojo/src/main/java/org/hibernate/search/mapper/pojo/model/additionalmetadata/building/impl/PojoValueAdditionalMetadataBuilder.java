/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

class PojoValueAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorValueNode {
	private PojoModelPathValueNode inverseSidePath;
	private boolean associationEmbedded = false;
	private Optional<ReindexOnUpdate> reindexOnUpdate = Optional.empty();

	@Override
	public void associationInverseSide(PojoModelPathValueNode inverseSidePath) {
		this.inverseSidePath = inverseSidePath;
	}

	@Override
	public void associationEmbedded() {
		this.associationEmbedded = true;
	}

	@Override
	public void indexingDependency(ReindexOnUpdate reindexOnUpdate) {
		this.reindexOnUpdate = Optional.of( reindexOnUpdate );
	}

	PojoValueAdditionalMetadata build() {
		return new PojoValueAdditionalMetadata( inverseSidePath, associationEmbedded, reindexOnUpdate );
	}
}
