/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

public interface DefaultedPropertyContext<T> {

	ConfigurationProperty<T> build();

}
