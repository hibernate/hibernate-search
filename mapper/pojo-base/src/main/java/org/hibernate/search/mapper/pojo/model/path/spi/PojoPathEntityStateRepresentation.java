/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.Optional;

public final class PojoPathEntityStateRepresentation {

	private final int ordinalInStateArray;
	private final Optional<BindablePojoModelPath> pathFromStateArrayElement;

	public PojoPathEntityStateRepresentation(int ordinalInStateArray,
			Optional<BindablePojoModelPath> pathFromStateArrayElement) {
		this.ordinalInStateArray = ordinalInStateArray;
		this.pathFromStateArrayElement = pathFromStateArrayElement;
	}

	public int ordinalInStateArray() {
		return ordinalInStateArray;
	}

	public Optional<BindablePojoModelPath> pathFromStateArrayElement() {
		return pathFromStateArrayElement;
	}
}
