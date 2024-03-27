/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Internal annotation to suppress some JQAssistant rules.
 * <p>
 * Note that rules must be specifically designed to take the annotation into account;
 * not all rules are.
 */
@Documented
@Target(TYPE)
@Retention(CLASS)
public @interface SuppressJQAssistant {

	String reason();

}
