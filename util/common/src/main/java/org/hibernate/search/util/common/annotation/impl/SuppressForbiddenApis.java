/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.annotation.impl;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Internal annotation to suppress forbidden-apis warnings/errors.
 */
@Documented
@Target({ TYPE, METHOD, CONSTRUCTOR })
@Retention(CLASS)
public @interface SuppressForbiddenApis {

	String reason();

}
