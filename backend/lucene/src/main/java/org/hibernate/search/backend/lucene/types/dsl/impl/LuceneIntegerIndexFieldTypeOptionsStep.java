/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneIntegerFieldCodec;


class LuceneIntegerIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneIntegerIndexFieldTypeOptionsStep, Integer> {

	LuceneIntegerIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Integer.class );
	}

	@Override
	protected LuceneIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Integer, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			Integer indexNullAsValue) {
		return new LuceneIntegerFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}
}
