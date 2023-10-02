/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.engine.backend.common.DocumentReference;

import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

/**
 * Utils useful when using mocks.
 */
public final class MockUtils {

	private MockUtils() {
	}

	public static DocumentReference referenceMatcher(DocumentReference expected) {
		DocumentReference normalizedExpected = normalize( expected );
		return ArgumentMatchers.argThat( new ArgumentMatcher<DocumentReference>() {
			@Override
			public boolean matches(DocumentReference argument) {
				return Objects.equals( normalizedExpected, normalize( argument ) );
			}

			@Override
			public String toString() {
				return String.valueOf( normalizedExpected );
			}
		} );
	}

	public static List<?> projectionMatcher(Object... expected) {
		return projectionMatcher( Arrays.asList( expected ) );
	}

	public static List<?> projectionMatcher(List<?> expected) {
		List<?> normalizedExpected = normalize( expected );
		return ArgumentMatchers.argThat( new ArgumentMatcher<List<?>>() {
			@Override
			public boolean matches(List<?> argument) {
				return Objects.equals( normalizedExpected, normalize( argument ) );
			}

			@Override
			public String toString() {
				return String.valueOf( normalizedExpected );
			}
		} );
	}

}
