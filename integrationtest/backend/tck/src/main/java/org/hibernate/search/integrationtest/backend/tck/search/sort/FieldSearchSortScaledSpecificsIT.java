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
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class FieldSearchSortScaledSpecificsIT {

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, 2 ) ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> incompatibleDecimalScaleIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, 5 ) ).name( "incompatibleDecimalScale" );

	@Before
	public void initData() {
		setupHelper.start().withIndexes( mainIndex, incompatibleDecimalScaleIndex ).setup();
	}

	@Test
	public void incompatibleDecimalScale() {
		StubMappingScope scope = mainIndex.createScope( incompatibleDecimalScaleIndex );

		Assertions.assertThatThrownBy(
				() -> scope.query().where( f -> f.matchAll() )
						.sort( f -> f.field( "scaledBigDecimal" ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort" )
				.hasMessageContaining( "'scaledBigDecimal'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleDecimalScaleIndex.name() )
				) );
	}

	private static class IndexBinding {
		final IndexFieldReference<BigDecimal> scaledBigDecimal;

		IndexBinding(IndexSchemaElement root, int scale) {
			scaledBigDecimal = root.field(
					"scaledBigDecimal",
					f -> f.asBigDecimal().decimalScale( scale )
			)
					.toReference();
		}
	}
}
