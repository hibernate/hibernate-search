/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public class PojoValueAdditionalMetadata {

	public static final PojoValueAdditionalMetadata EMPTY = new PojoValueAdditionalMetadata(
			null, false, Optional.empty(), Collections.emptySet(), null
	);

	private final PojoModelPathValueNode inverseSidePath;
	private final boolean associationEmbedded;
	private final Optional<ReindexOnUpdate> reindexOnUpdate;
	private final Set<PojoModelPathValueNode> derivedFrom;
	private final Integer decimalScale;

	public PojoValueAdditionalMetadata(PojoModelPathValueNode inverseSidePath, boolean associationEmbedded,
			Optional<ReindexOnUpdate> reindexOnUpdate, Set<PojoModelPathValueNode> derivedFrom, Integer decimalScale) {
		this.inverseSidePath = inverseSidePath;
		this.associationEmbedded = associationEmbedded;
		this.reindexOnUpdate = reindexOnUpdate;
		this.derivedFrom = derivedFrom;
		this.decimalScale = decimalScale;
	}

	public Optional<PojoModelPathValueNode> getInverseSidePath() {
		return Optional.ofNullable( inverseSidePath );
	}

	public boolean isAssociationEmbedded() {
		return associationEmbedded;
	}

	public Optional<ReindexOnUpdate> getReindexOnUpdate() {
		return reindexOnUpdate;
	}

	public Set<PojoModelPathValueNode> getDerivedFrom() {
		return derivedFrom;
	}

	public Integer getDecimalScale() {
		return decimalScale;
	}
}
