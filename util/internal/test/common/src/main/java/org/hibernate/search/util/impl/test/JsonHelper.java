/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test;

import static org.assertj.core.api.Assertions.fail;

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
		assertJsonEquals( expectedJson, actualJson, JSONCompareMode.STRICT );
	}

	public static void assertJsonEqualsIgnoringUnknownFields(String expectedJson, String actualJson) {
		assertJsonEquals( expectedJson, actualJson, JSONCompareMode.STRICT_ORDER );
	}

	public static void assertJsonEquals(String expectedJson, String actualJson, JSONCompareMode mode) {
		try {
			JSONCompareResult result = JSONCompare.compareJSON( expectedJson, actualJson, mode );

			if ( result.failed() ) {
				fail( result.getMessage() + "; Actual: " + actualJson );
				throw new IllegalStateException( "This should never happen" );
			}
		}
		catch (JSONException e) {
			throw new RuntimeException( e );
		}
	}
}
