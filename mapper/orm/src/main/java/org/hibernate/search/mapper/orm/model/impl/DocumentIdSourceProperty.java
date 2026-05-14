/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.accessor.HibernateAccessorValueReader;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

public class DocumentIdSourceProperty<I> {
	public final Class<? super I> clazz;
	public final String name;
	public final HibernateAccessorValueReader<I> handle;

	public DocumentIdSourceProperty(PojoPropertyModel<I> documentIdSourceProperty) {
		this.clazz = documentIdSourceProperty.typeModel().rawType().typeIdentifier().javaClass();
		this.name = documentIdSourceProperty.name();
		this.handle = documentIdSourceProperty.handle();
	}
}
