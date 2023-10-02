/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Optional;

import org.hibernate.search.util.common.impl.Contracts;

public final class ExpectationsAlternative<S, U> {

	public static <S, U> ExpectationsAlternative<S, U> supported(S supportedExpectations) {
		Contracts.assertNotNull( supportedExpectations, "supportedExpectations" );
		return new ExpectationsAlternative<>( supportedExpectations, null );
	}

	public static <S, U> ExpectationsAlternative<S, U> unsupported(U unsupportedExpectations) {
		Contracts.assertNotNull( unsupportedExpectations, "unsupportedExpectations" );
		return new ExpectationsAlternative<>( null, unsupportedExpectations );
	}

	private final S supportedExpectations;
	private final U unsupportedExpectations;

	private ExpectationsAlternative(S supportedExpectations, U unsupportedExpectations) {
		this.supportedExpectations = supportedExpectations;
		this.unsupportedExpectations = unsupportedExpectations;
	}

	public boolean isSupported() {
		return supportedExpectations != null;
	}

	public Optional<S> getSupported() {
		return Optional.ofNullable( supportedExpectations );
	}

	public Optional<U> getUnsupported() {
		return Optional.ofNullable( unsupportedExpectations );
	}

}
