/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations;

public class IndexNullAsExpectactions<F> {

	private final F indexNullAsValue;
	private final F differentValue;

	public IndexNullAsExpectactions(F indexNullAsValue, F differentValue) {
		this.indexNullAsValue = indexNullAsValue;
		this.differentValue = differentValue;
	}

	public final F getIndexNullAsValue() {
		return indexNullAsValue;
	}

	public final F getDifferentValue() {
		return differentValue;
	}
}
