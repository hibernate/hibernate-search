/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.data;

import java.nio.charset.StandardCharsets;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public final class TextContent {

	private TextContent() {
	}

	public static CharSource greatExpectations() {
		return Resources.asCharSource(
				TextContent.class.getResource( "/great_expectations.txt" ),
				StandardCharsets.UTF_8
		);
	}

}
