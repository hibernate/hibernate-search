/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public interface PojoMethodParameterModel<T> {

	int index();

	Optional<String> name();

	Stream<Annotation> annotations();

	PojoTypeModel<T> typeModel();

	/**
	 * @return {@code true} if this parameter is expected to receive an "enclosing instance",
	 * e.g. an instance of the enclosing class in the case of Java inner classes or method-local classes.
	 */
	boolean isEnclosingInstance();

	/**
	 * @return {@code true} if {@code obj} is a {@link MappableTypeModel} referencing the exact same type
	 * with the exact same exposed metadata.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}
