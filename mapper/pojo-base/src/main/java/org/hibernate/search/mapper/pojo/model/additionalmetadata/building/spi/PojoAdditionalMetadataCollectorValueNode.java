/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public interface PojoAdditionalMetadataCollectorValueNode extends PojoAdditionalMetadataCollector {

	void associationInverseSide(PojoModelPathValueNode inverseSidePath);

	void associationEmbedded();

	void reindexOnUpdate(ReindexOnUpdate reindexOnUpdate);

	void derivedFrom(Set<PojoModelPathValueNode> path);

	void decimalScale(int decimalScale);
}
