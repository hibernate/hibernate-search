/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.work;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.hibernate.search.backend.lucene.cfg.spi.LuceneBackendSpiSettings;
import org.hibernate.search.backend.lucene.work.spi.LuceneWorkExecutorProvider;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LuceneBackendWorkExecutorProviderIT {

	@Mock
	private LuceneWorkExecutorProvider backendWorkExecutorProvider;

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Test
	void test() {
		when( backendWorkExecutorProvider.writeExecutor( any() ) ).thenReturn(
				new DelegatingSimpleScheduledExecutor( new ScheduledThreadPoolExecutor( 1 ), true )
		);
		setupHelper.start()
				.withIndex( index )
				.withBackendProperty( LuceneBackendSpiSettings.Radicals.BACKEND_WORK_EXECUTOR_PROVIDER, backendWorkExecutorProvider )
				.setup();

		verify( backendWorkExecutorProvider ).writeExecutor( any() );
	}


	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}
}
