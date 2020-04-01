/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import java.math.BigDecimal;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class FieldSearchSortScaledSpecificsIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME = "IndexWithIncompatibleDecimalScale";

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping incompatibleDecimalScaleIndexMapping;
	private StubMappingIndexManager incompatibleDecimalScaleIndexManager;

	@Before
	public void initData() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME,
						ctx -> incompatibleDecimalScaleIndexMapping = new IndexMapping( ctx.getSchemaElement(), 5 ),
						indexManager -> this.incompatibleDecimalScaleIndexManager = indexManager
				)
				.setup();
	}

	@Test
	public void incompatibleDecimalScale() {
		StubMappingScope scope = indexManager.createScope( incompatibleDecimalScaleIndexManager );

		Assertions.assertThatThrownBy(
				() -> scope.query().where( f -> f.matchAll() )
						.sort( f -> f.field( "scaledBigDecimal" ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'scaledBigDecimal'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_DECIMAL_SCALE_INDEX_NAME )
				) );
	}

	private static class IndexMapping {
		final IndexFieldReference<BigDecimal> scaledBigDecimal;

		IndexMapping(IndexSchemaElement root, int scale) {
			scaledBigDecimal = root.field(
					"scaledBigDecimal",
					f -> f.asBigDecimal().decimalScale( scale )
			)
					.toReference();
		}
	}
}
