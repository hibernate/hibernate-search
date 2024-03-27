/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

class PojoValueAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorValueNode {
	private PojoModelPathValueNode inverseSidePath;
	private boolean associationEmbedded = false;
	private Optional<ReindexOnUpdate> reindexOnUpdate = Optional.empty();
	private Set<PojoModelPathValueNode> derivedFrom = Collections.emptySet();
	private Integer decimalScale;

	@Override
	public void associationInverseSide(PojoModelPathValueNode inverseSidePath) {
		this.inverseSidePath = inverseSidePath;
	}

	@Override
	public void associationEmbedded() {
		this.associationEmbedded = true;
	}

	@Override
	public void reindexOnUpdate(ReindexOnUpdate reindexOnUpdate) {
		this.reindexOnUpdate = Optional.of( reindexOnUpdate );
	}

	@Override
	public void derivedFrom(Set<PojoModelPathValueNode> derivedFrom) {
		this.derivedFrom = derivedFrom;
	}

	@Override
	public void decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
	}

	PojoValueAdditionalMetadata build() {
		return new PojoValueAdditionalMetadata(
				inverseSidePath, associationEmbedded, reindexOnUpdate, derivedFrom, decimalScale
		);
	}
}
