/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testutil.backend.elasticsearch;

import org.hibernate.search.exception.AssertionFailure;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Helper functionality around JSON documents.
 *
 * @author Gunnar Morling
 */
public class JsonHelper {

	private JsonHelper() {
	}

	public static void assertJsonEquals(String expectedJson, String actualJson) {
		try {
			JSONCompareResult result = JSONCompare.compareJSON( expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE );

			if ( result.failed() ) {
				throw new AssertionFailure( result.getMessage() + "; Actual: " + actualJson );
			}
		}
		catch (JSONException e) {
			throw new RuntimeException( e );
		}
	}
}
