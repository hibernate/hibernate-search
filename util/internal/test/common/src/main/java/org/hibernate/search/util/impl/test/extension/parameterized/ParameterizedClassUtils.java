/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.util.ReflectionUtils;

final class ParameterizedClassUtils {

	private ParameterizedClassUtils() {
	}

	static boolean isActualTestMethodToExecute(Method candidate) {
		return isAnnotated( candidate, Test.class ) || isAnnotated( candidate, ParameterizedTest.class );
	}

	static boolean isParameterizedSetup(Method candidate) {
		return isAnnotated( candidate, ParameterizedSetup.class );
	}

	static void findParameters(List<Object[]> arguments, ExtensionContext extensionContext, Method testMethod) {
		for ( ArgumentsSource source : findRepeatableAnnotations( testMethod, ArgumentsSource.class ) ) {
			ArgumentsProvider argumentsProvider =
					AnnotationConsumerInitializer.initialize( testMethod, ReflectionUtils.newInstance( source.value() ) );
			try {
				arguments.addAll(
						argumentsProvider.provideArguments( extensionContext )
								.map( Arguments::get )
								.collect( Collectors.toList() ) );
			}
			catch (Exception e) {
				throw new IllegalStateException( "unable to read arguments.", e );
			}
		}
	}
}
