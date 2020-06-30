/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

public abstract class AbstractPredicateDataSet {

	protected final String routingKey;

	protected AbstractPredicateDataSet(String routingKey) {
		this.routingKey = routingKey;
	}

	@Override
	public String toString() {
		if ( routingKey != null ) {
			// Pretty rendering of the dataset as a parameter of the Parameterized runner
			return routingKey;
		}
		else {
			// Probably not used as a parameter of the Parameterized runner
			return getClass().getName();
		}
	}

	public String docId(int docOrdinal) {
		return routingKey + "_doc_" + docOrdinal;
	}

}
