/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks test classes that carry class-level parameters.
 * <p>
 * Such test classes must have a setup method annotated with {@link ParameterizedSetup}.
 * <p>
 * Prefer using {@link ParameterizedPerClass} or {@link ParameterizedPerMethod} instead of this one.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ParameterizedClassCleanupExtension.class)
public @interface ParameterizedClass {
}
