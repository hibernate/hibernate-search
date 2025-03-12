/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

public interface PojoInjectablePropertyModel<T> extends PojoPropertyModel<T> {

	/**
	 * @return A handle to read the value of this property on a instance of its hosting type.
	 */
	ValueReadWriteHandle<T> handle();

}
