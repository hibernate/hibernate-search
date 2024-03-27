/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.spi;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * Session-scoped information and operations for use in {@link PojoImplicitReindexingResolver}.
 */
public interface PojoImplicitReindexingResolverSessionContext {

	PojoRuntimeIntrospector runtimeIntrospector();

}
