/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.inject.scope.spi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

/**
 * Scope for job execution, to be mapped to the scopes specific to each Jakarta Batch implementation.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
@Scope
@Inherited
public @interface HibernateSearchJobScoped {

}
