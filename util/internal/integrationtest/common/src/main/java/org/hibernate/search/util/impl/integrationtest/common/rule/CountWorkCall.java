/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Set;

public class CountWorkCall extends Call<CountWorkCall> {

	private final Set<String> indexNames;
	private final Long expectedResult;

	CountWorkCall(Set<String> indexNames, Long expectedResult) {
		this.indexNames = indexNames;
		this.expectedResult = expectedResult;
	}

	public CallBehavior<Long> verify(CountWorkCall actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Count work did not target the expected indexes: " )
				.isEqualTo( indexNames );

		assertThat( actualCall.expectedResult )
				.as( "Actual Call should not carry an expectedResult" )
				.isNull();

		return () -> expectedResult;
	}

	@Override
	protected boolean isSimilarTo(CountWorkCall other) {
		return Objects.equals( indexNames, other.indexNames );
	}

	@Override
	protected String summary() {
		return "search work execution on indexes '" + indexNames + "'";
	}
}
