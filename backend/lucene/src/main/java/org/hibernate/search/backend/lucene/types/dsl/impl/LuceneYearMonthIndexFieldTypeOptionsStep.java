/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.YearMonth;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneYearMonthFieldCodec;

class LuceneYearMonthIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneYearMonthIndexFieldTypeOptionsStep, YearMonth> {

	LuceneYearMonthIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class );
	}

	@Override
	protected LuceneYearMonthIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<YearMonth, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			YearMonth indexNullAsValue) {
		return new LuceneYearMonthFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}
}
