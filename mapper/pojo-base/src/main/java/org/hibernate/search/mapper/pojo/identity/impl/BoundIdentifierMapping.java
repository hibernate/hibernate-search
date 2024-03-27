/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class BoundIdentifierMapping<I, E> {
	public final IdentifierMappingImplementor<I, E> mapping;
	public final PojoTypeModel<I> identifierType;
	public final Optional<PojoPropertyModel<I>> documentIdSourceProperty;

	public BoundIdentifierMapping(IdentifierMappingImplementor<I, E> mapping, PojoTypeModel<I> identifierType,
			Optional<PojoPropertyModel<I>> documentIdSourceProperty) {
		this.mapping = mapping;
		this.identifierType = identifierType;
		this.documentIdSourceProperty = documentIdSourceProperty;
	}

	public IdentifierMappingImplementor<I, E> mapping() {
		return mapping;
	}
}
