/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.work.spi.ElasticsearchWorkExecutorProvider;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class ElasticsearchBackendWorkExecutorProviderIT {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ElasticsearchWorkExecutorProvider backendWorkExecutorProvider;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Test
	public void test() {
		when( backendWorkExecutorProvider.workExecutor( any() ) ).thenReturn(
				new DelegatingSimpleScheduledExecutor( new ScheduledThreadPoolExecutor( 1 ), true )
		);
		setupHelper.start()
				.withIndex( index )
				.withBackendProperty( ElasticsearchBackendSpiSettings.Radicals.BACKEND_WORK_EXECUTOR_PROVIDER, backendWorkExecutorProvider )
				.setup();

		verify( backendWorkExecutorProvider ).workExecutor( any() );
	}


	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}
}
