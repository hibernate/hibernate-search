/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

public interface ProjectionMappedTypeContext {

	String name();

	Class<?> javaClass();

	boolean loadingAvailable();

}
