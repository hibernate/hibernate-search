/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IndexManagerIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final StubMappedIndex index = StubMappedIndex.withoutFields();

	private static Backend backendApi;
	private static IndexManager indexApi;

	@BeforeAll
	static void setup() {
		StubMapping mapping = setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
		backendApi = mapping.integration().backend();
		indexApi = index.toApi();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void backend() {
		assertThat( indexApi.backend() )
				.isNotNull()
				.isSameAs( backendApi );
	}
}
