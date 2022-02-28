/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.util.impl.test.data.TextContent;

import com.google.common.io.CharSource;

public final class Datasets {

	public static final String CONSTANT_TEXT = "constant-text";

	public static final String GREAT_EXPECTATIONS = "hibernate-dev-ml-2016-01";

	private Datasets() {
	}

	public static Dataset createDataset(String name)
			throws IOException {
		switch ( name ) {
			case CONSTANT_TEXT:
				return new ConstantDataset();
			case GREAT_EXPECTATIONS:
				return new SampleDataset( parseSimple( TextContent.greatExpectations() ) );
			default:
				throw new IllegalArgumentException( "Unknown dataset: " + name );
		}
	}

	private static List<SampleDataset.DataSample> parseSimple(CharSource charSource) throws IOException {
		return charSource.readLines( new SimpleDataSampleParser() );
	}

}
