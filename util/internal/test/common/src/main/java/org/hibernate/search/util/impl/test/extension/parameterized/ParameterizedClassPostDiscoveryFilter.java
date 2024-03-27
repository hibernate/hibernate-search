/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedClassUtils.isParameterizedSetup;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.search.util.impl.test.extension.AbstractScopeTrackingExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;

/**
 * Launcher post discovery filter that will not include tests annotated with {@link org.junit.jupiter.api.Test} or {@link org.junit.jupiter.params.ParameterizedTest}
 * in a regular test run if the class they are located in is a {@link ParameterizedClass} type of test.
 * These tests will be discovered by the extension itself, and it'll handle their execution.
 */
public class ParameterizedClassPostDiscoveryFilter implements org.junit.platform.launcher.PostDiscoveryFilter {
	@Override
	public FilterResult apply(TestDescriptor testDescriptor) {
		if ( testDescriptor instanceof ClassTestDescriptor ) {
			Class<?> testClass = ( (ClassTestDescriptor) testDescriptor ).getTestClass();
			if ( isParameterizedClass( testClass ) ) {
				// class annotated with a ParameterizedSetup but no setup method with ParameterizedSetup -- we better fail :)
				if ( !hasParameterizedSetup( testClass ) ) {
					throw new IllegalStateException(
							"Unable to find a @ParameterizedSetup method in " + testClass
									+ ". @ParameterizedClass tests MUST have a @ParameterizedSetup method." );
				}
				// parameterized class has some @BeforeEach callbacks. These are not "supported by the @ParameterizedClass
				if ( hasBeforeEach( testClass ) ) {
					throw new IllegalStateException(
							"@ParameterizedClass test " + testClass
									+ " contains some methods annotated with @BeforeEach. " +
									"Use @ParameterizedSetupBeforeTest instead." );
				}
				checkNonStaticOrmHelper( testClass );
			}
		}
		// nested class inside a parameterized test -- we are not handling this option for now, so.... failing:
		if ( testDescriptor instanceof NestedClassTestDescriptor ) {
			if ( isParameterizedClass( ( (NestedClassTestDescriptor) testDescriptor ).getTestClass().getEnclosingClass() ) ) {
				throw new IllegalStateException(
						"@Nested test classes are not supported within a @ParameterizedClass tests" );
			}
		}
		if ( testDescriptor instanceof MethodBasedTestDescriptor ) {
			MethodBasedTestDescriptor methodTestDescriptor = (MethodBasedTestDescriptor) testDescriptor;

			boolean parameterizedClass = isParameterizedClass( methodTestDescriptor.getTestClass() );
			boolean parameterizedSetup = isParameterizedSetup( methodTestDescriptor.getTestMethod() );
			if ( parameterizedSetup ) {
				// we found a setup method in a non-parameterized class -- failing
				if ( !parameterizedClass ) {
					throw new IllegalStateException(
							"Test class " + methodTestDescriptor.getTestClass()
									+ " is using a @ParameterizedSetup, but is not annotated with a @ParameterizedClass!" );
				}
			}
			else if ( parameterizedClass ) {
				// we are looking at something inside a parameterized class, hence we want to make sure it is a supported descriptor
				// i.e. it is a @Test or a @ParameterizedTest and if so we will exclude the method from JUnit run.
				// But if it is something else -- we will fail as we are not handling other descriptors at the moment
				if ( ParameterizedClassUtils.isActualTestMethodToExecute( methodTestDescriptor.getTestMethod() ) ) {
					return FilterResult
							.excluded(
									"It is a test method that ParameterizedClassExtension will discover and execute itself." );
				}
				else {
					throw new IllegalStateException(
							"This type of tests are not yet supported by the parameterized class test extension."
									+ " Remove any JUnit @TestTemplate/@TestFactory annotations from "
									+ methodTestDescriptor.getTestMethod() );
				}
			}
		}
		return FilterResult.included( "This filter does not care about this case." );
	}

	private boolean checkNonStaticOrmHelper(Class<?> testClass) {
		if ( Object.class.equals( testClass ) ) {
			return false;
		}
		for ( Field field : testClass.getDeclaredFields() ) {
			if ( isAnnotated( field, RegisterExtension.class )
					&& AbstractScopeTrackingExtension.class.isAssignableFrom( field.getType() )
					&& !Modifier.isStatic( field.getModifiers() ) ) {
				throw new IllegalStateException( "Scope tracking extension " + field.getName()
						+ " must be declared as static field in a @ParameterizedClass test " + testClass.getName() + "." );
			}
		}

		return checkNonStaticOrmHelper( testClass.getSuperclass() );
	}

	private boolean hasParameterizedSetup(Class<?> testClass) {
		return hasMethodAnnotated( testClass, ParameterizedSetup.class );
	}

	private boolean hasBeforeEach(Class<?> testClass) {
		return hasMethodAnnotated( testClass, BeforeEach.class );
	}

	private boolean hasMethodAnnotated(Class<?> testClass, Class<? extends Annotation> annotationType) {
		if ( Object.class.equals( testClass ) ) {
			return false;
		}
		for ( Method method : testClass.getDeclaredMethods() ) {
			if ( isAnnotated( method, annotationType ) ) {
				findAnnotation( method, annotationType );
				return true;
			}
		}

		return hasMethodAnnotated( testClass.getSuperclass(), annotationType );
	}

	private static boolean isParameterizedClass(Class<?> testClass) {
		return isAnnotated( testClass, ParameterizedClass.class );
	}
}
