/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneByteFieldCodec;

class LuceneByteIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneByteIndexFieldTypeOptionsStep, Byte> {

	LuceneByteIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Byte.class );
	}

	@Override
	protected LuceneByteIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Byte, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			Byte indexNullAsValue) {
		return new LuceneByteFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}
}
