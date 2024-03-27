/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.TestInstance;

/**
 * Marks test classes that carry class-level parameters, requiring a
 * <p>
 * Such test classes must have a setup method annotated with {@link ParameterizedSetup}.
 * <p>
 * Configuration will be initialized once per set of executed tests.
 * Initialization will happen just before executing the first test in the group.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ParameterizedClass
public @interface ParameterizedPerClass {
}
