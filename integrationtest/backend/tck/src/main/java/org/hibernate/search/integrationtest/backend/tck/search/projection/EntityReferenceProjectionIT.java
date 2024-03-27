/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

class EntityReferenceProjectionIT extends AbstractEntityReferenceProjectionIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final StubMappedIndex mainIndex = StubMappedIndex.withoutFields();

	public EntityReferenceProjectionIT() {
		super( mainIndex );
	}

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( mainIndex ).setup();

		BulkIndexer indexer = mainIndex.bulkIndexer();
		initData( indexer );
		indexer.join();
	}

	@Override
	public <R, E, LOS> SearchQueryWhereStep<?, R, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
		return step.select( f -> f.entityReference() );
	}
}
