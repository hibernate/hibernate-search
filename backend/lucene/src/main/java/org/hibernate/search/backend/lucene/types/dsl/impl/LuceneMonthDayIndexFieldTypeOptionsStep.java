/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.MonthDay;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneMonthDayFieldCodec;

class LuceneMonthDayIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	LuceneMonthDayIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class );
	}

	@Override
	protected LuceneMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<MonthDay, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			MonthDay indexNullAsValue) {
		return new LuceneMonthDayFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}
}
