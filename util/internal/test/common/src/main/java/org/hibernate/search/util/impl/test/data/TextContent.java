/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class TextContent {

	private TextContent() {
	}

	public static void greatExpectations(Consumer<String> consumer) throws IOException {
		try ( InputStream resource = TextContent.class.getResourceAsStream( "/great_expectations.txt" ) ) {
			if ( resource == null ) {
				throw new IllegalStateException( "Text resource not found" );
			}
			try ( InputStreamReader reader = new InputStreamReader( resource, StandardCharsets.UTF_8 );
					BufferedReader bufferedReader = new BufferedReader( reader ) ) {
				bufferedReader.lines().forEach( consumer );
			}
		}
	}

	public static String readGreatExpectations() throws IOException {
		StringBuilder sb = new StringBuilder();
		greatExpectations( sb::append );
		return sb.toString();
	}

}
