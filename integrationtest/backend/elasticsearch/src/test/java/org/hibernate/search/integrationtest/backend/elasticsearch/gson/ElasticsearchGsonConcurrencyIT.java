/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.gson;

import java.io.IOException;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Reproducer for HSEARCH-3725,
 * which is caused by a bug in Gson: https://github.com/google/gson/issues/764
 * <p>
 * The bug happens very rarely, so we run the test multiple times,
 * but there's no guarantee we will actually reproduce it.
 * On my machine (8 CPU cores), I've seen the test run hundreds of times in a row
 * without the bug occurring even once.
 * But sometimes, if you're lucky, it will occur (I've seen it).
 * <p>
 * For the bug to occur, we need to:
 * <ul>
 *     <li>Initialize multiple indexes in parallel</li>
 *     <li>Serialize a mapping, or at least convert it to JsonElements, as part of index initialization</li>
 * </ul>
 * If we reach some state where two threads are initializing the same Gson TypeAdapter simultaneously,
 * there is a small chance FutureTypeAdapter#write(JsonWriter, Object)
 * will see a null adapter and throw an exception.
 */
@TestForIssue(jiraKey = "HSEARCH-3725")
class ElasticsearchGsonConcurrencyIT {

	/*
	 * Please keep these constants reasonably low so that routine builds don't take forever:
	 * test duration will be proportional to REPRODUCER_ATTEMPTS*INDEX_COUNT_PER_ATTEMPT.
	 * However, you will need to raise them in your local copy of the code
	 * to have a high chance of reproducing the bug.
	 */
	private static final int REPRODUCER_ATTEMPTS = 20;
	// This must be at least 2, but you don't need more than the number of CPU cores.
	private static final int INDEX_COUNT_PER_ATTEMPT = 4;

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@Test
	void repeatedlyStartMultipleIndexesSerializingWithGsonInParallel() throws IOException {
		for ( int i = 0; i < REPRODUCER_ATTEMPTS; i++ ) {
			startMultipleIndexesSerializingWithGsonInParallel();
			setupHelper.cleanUp();
		}
	}

	private void startMultipleIndexesSerializingWithGsonInParallel() {
		SearchSetupHelper.SetupContext setupCtx = setupHelper.start();

		for ( int i = 0; i < INDEX_COUNT_PER_ATTEMPT; i++ ) {
			setupCtx = setupCtx.withIndex(
					StubMappedIndex.ofNonRetrievable( IndexBinding::new ).name( "IndexName_" + i )
			);
		}

		setupCtx.setup();
	}

	private static class IndexBinding {
		final ObjectBinding child;

		IndexBinding(IndexSchemaElement root) {
			// Two levels of nested properties are necessary to reproduce the Gson bug causing HSEARCH-3725
			child = new ObjectBinding( root, "child", 3 );
		}
	}

	private static class ObjectBinding {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> text;
		final ObjectBinding child;

		ObjectBinding(IndexSchemaElement parent, String name, int depth) {
			IndexSchemaObjectField objectField = parent.objectField( name );
			self = objectField.toReference();
			text = objectField.field(
					"text",
					f -> f.asString()
			)
					.toReference();
			if ( depth > 1 ) {
				child = new ObjectBinding( objectField, name, depth - 1 );
			}
			else {
				child = null;
			}
		}
	}

}
