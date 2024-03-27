/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface BeanCreationContext {

	BeanResolver beanResolver();

}
