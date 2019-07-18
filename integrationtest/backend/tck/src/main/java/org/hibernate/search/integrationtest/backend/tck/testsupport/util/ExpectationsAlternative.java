/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
