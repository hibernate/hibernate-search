/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class StandalonePojoContainedTypeContext<E> extends AbstractStandalonePojoTypeContext<E> {

	private StandalonePojoContainedTypeContext(Builder<E> builder) {
		super( builder );
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoContainedTypeExtendedMappingCollector {
		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
			super( typeIdentifier, entityName );
		}

		StandalonePojoContainedTypeContext<E> build() {
			return new StandalonePojoContainedTypeContext<>( this );
		}
	}
}
