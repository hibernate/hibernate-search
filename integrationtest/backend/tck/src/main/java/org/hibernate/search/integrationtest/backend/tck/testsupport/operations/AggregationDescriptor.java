/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;

public abstract class AggregationDescriptor {

	private static List<AggregationDescriptor> all;

	public static List<AggregationDescriptor> getAll() {
		if ( all == null ) {
			all = Collections.unmodifiableList( Arrays.asList(
					RangeAggregationDescriptor.INSTANCE,
					TermsAggregationDescriptor.INSTANCE
			) );
		}
		return all;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public abstract <F> ExpectationsAlternative<
			SupportedSingleFieldAggregationExpectations<F>,
			UnsupportedSingleFieldAggregationExpectations> getSingleFieldAggregationExpectations(
					FieldTypeDescriptor<F> typeDescriptor);

}
