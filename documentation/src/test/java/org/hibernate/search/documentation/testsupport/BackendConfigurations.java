/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

public final class BackendConfigurations {

	private BackendConfigurations() {
	}

	public static List<BackendConfiguration> simple() {
		return Arrays.asList(
				new LuceneBackendConfiguration(),
				new ElasticsearchBackendConfiguration()
		);
	}

}
