/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class DocumentIdSourceProperty<I> {
	public final Class<? super I> clazz;
	public final String name;
	public final ValueReadHandle<I> handle;

	public DocumentIdSourceProperty(PojoPropertyModel<I> documentIdSourceProperty) {
		this.clazz = documentIdSourceProperty.typeModel().rawType().typeIdentifier().javaClass();
		this.name = documentIdSourceProperty.name();
		this.handle = documentIdSourceProperty.handle();
	}
}
