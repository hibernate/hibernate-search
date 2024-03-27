/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmDynamicMapPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmBootstrapIntrospector introspector;
	private final HibernateOrmDynamicMapRawTypeModel holderTypeModel;

	private final String name;
	private final HibernateOrmBasicDynamicMapPropertyMetadata ormPropertyMetadata;

	private ValueReadHandle<T> handle;
	private PojoTypeModel<T> typeModel;

	HibernateOrmDynamicMapPropertyModel(HibernateOrmBootstrapIntrospector introspector,
			HibernateOrmDynamicMapRawTypeModel holderTypeModel,
			String name,
			HibernateOrmBasicDynamicMapPropertyMetadata ormPropertyMetadata) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.ormPropertyMetadata = ormPropertyMetadata;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Stream<Annotation> annotations() {
		return Stream.empty();
	}

	@Override
	@SuppressWarnings("unchecked") // We will just trust ORM metadata on this one.
	public PojoTypeModel<T> typeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = (PojoTypeModel<T>) ormPropertyMetadata.getTypeModelFactory().create( introspector );
			}
			catch (RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e.getMessage(), e );
			}
		}
		return typeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // We will just trust ORM metadata on this one.
	public ValueReadHandle<T> handle() {
		if ( handle == null ) {
			try {
				handle = (ValueReadHandle<T>) new HibernateOrmDynamicMapValueReadHandle<>(
						name, typeModel().rawType().typeIdentifier().javaClass()
				);
			}
			catch (RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e.getMessage(), e );
			}
		}
		return handle;
	}

}
