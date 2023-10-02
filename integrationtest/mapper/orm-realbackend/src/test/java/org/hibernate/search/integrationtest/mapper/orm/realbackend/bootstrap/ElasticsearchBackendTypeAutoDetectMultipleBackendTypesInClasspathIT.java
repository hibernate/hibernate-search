/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
