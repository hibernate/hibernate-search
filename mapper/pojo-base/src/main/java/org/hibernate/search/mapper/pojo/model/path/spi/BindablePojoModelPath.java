/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public final class BindablePojoModelPath {
	private final PojoTypeModel<?> rootType;
	private final PojoModelPathValueNode path;

	public BindablePojoModelPath(PojoTypeModel<?> rootType, PojoModelPathValueNode path) {
		this.rootType = rootType;
		this.path = path;
	}

	public PojoTypeModel<?> rootType() {
		return rootType;
	}

	public PojoModelPathValueNode path() {
		return path;
	}
}
