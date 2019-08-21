/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalTimeFieldCodec;

class LuceneLocalTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneLocalTimeIndexFieldTypeOptionsStep, LocalTime> {

	LuceneLocalTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class );
	}

	@Override
	protected LuceneLocalTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<LocalTime, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			LocalTime indexNullAsValue) {
		return new LuceneLocalTimeFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}
}
