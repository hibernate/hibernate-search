/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FieldProjectionExpectations<F> {

	private final F document1Value;
	private final F document2Value;
	private final F document3Value;

	public FieldProjectionExpectations(F document1Value, F document2Value, F document3Value) {
		this.document1Value = document1Value;
		this.document2Value = document2Value;
		this.document3Value = document3Value;
	}

	public final F getDocument1Value() {
		return document1Value;
	}

	public final F getDocument2Value() {
		return document2Value;
	}

	public final F getDocument3Value() {
		return document3Value;
	}

}
