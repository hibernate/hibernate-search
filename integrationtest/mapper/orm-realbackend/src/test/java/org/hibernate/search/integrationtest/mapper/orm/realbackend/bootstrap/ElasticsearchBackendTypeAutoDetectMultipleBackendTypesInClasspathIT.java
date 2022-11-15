/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.bootstrap;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;

/**
 * Checks that Hibernate Search will fail to auto-detect the backend type and offer suggestions
 * if there are multiple backend types in the classpath.
 * Also checks that setting the property "hibernate.search.backend.type" will solve the problem.
 */
class ElasticsearchBackendTypeAutoDetectMultipleBackendTypesInClasspathIT
		extends
		AbstractBackendTypeAutoDetectMultipleBackendTypesInClasspathIT {
	protected ElasticsearchBackendTypeAutoDetectMultipleBackendTypesInClasspathIT() {
		super( "elasticsearch", new ElasticsearchBackendConfiguration() );
	}
}
