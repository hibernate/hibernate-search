/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.Instant;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneInstantFieldCodec;

class LuceneInstantIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneInstantIndexFieldTypeOptionsStep, Instant> {

	LuceneInstantIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class );
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Instant, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			Instant indexNullAsValue) {
		return new LuceneInstantFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}

	@Override
	protected LuceneInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
