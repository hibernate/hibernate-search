/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
