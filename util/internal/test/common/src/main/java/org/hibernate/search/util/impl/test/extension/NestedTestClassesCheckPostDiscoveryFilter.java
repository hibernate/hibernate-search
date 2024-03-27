/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension;

import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;

/**
 * This filter is here to make sure that we didn't add a {@link Nested @Nested test class} as a static by accident.
 * <p>
 * {@link Nested @Nested test classes} must be nonstatic as otherwise they won't be executed.
 */
public class NestedTestClassesCheckPostDiscoveryFilter implements org.junit.platform.launcher.PostDiscoveryFilter {
	@Override
	public FilterResult apply(TestDescriptor testDescriptor) {
		if ( testDescriptor instanceof ClassBasedTestDescriptor ) {
			Class<?> testClass = ( (ClassBasedTestDescriptor) testDescriptor ).getTestClass();
			// We want to double-check that we don't have any static classes with @Nested annotation:
			for ( Class<?> inner : testClass.getDeclaredClasses() ) {
				if ( Modifier.isStatic( inner.getModifiers() ) && isAnnotated( inner, Nested.class ) ) {
					throw new IllegalStateException(
							"Nested test classes annotated with @Nested cannot be static, as they won't be executed. "
									+ "Make " + inner + " non-static or remove the annotation (@Nested)." );
				}
			}
		}
		return FilterResult.included( "This filter includes all the tests." );
	}
}
