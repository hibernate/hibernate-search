/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FieldSortScaledSpecificsIT {

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, 2 ) ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> incompatibleDecimalScaleIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, 5 ) ).name( "incompatibleDecimalScale" );

	@BeforeEach
	void initData() {
		setupHelper.start().withIndexes( mainIndex, incompatibleDecimalScaleIndex ).setup();
	}

	@Test
	void incompatibleDecimalScale() {
		StubMappingScope scope = mainIndex.createScope( incompatibleDecimalScaleIndex );

		String fieldPath = "scaledBigDecimal";

		assertThatThrownBy(
				() -> scope.query().where( f -> f.matchAll() )
						.sort( f -> f.field( fieldPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'sort:field'",
						"Field codec differs:", "decimalScale=2", " vs. ", "decimalScale=5"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleDecimalScaleIndex.name() )
				) );
	}

	private static class IndexBinding {
		final IndexFieldReference<BigDecimal> scaledBigDecimal;

		IndexBinding(IndexSchemaElement root, int scale) {
			scaledBigDecimal = root.field(
					"scaledBigDecimal",
					f -> f.asBigDecimal().decimalScale( scale ).sortable( Sortable.YES )
			)
					.toReference();
		}
	}
}
