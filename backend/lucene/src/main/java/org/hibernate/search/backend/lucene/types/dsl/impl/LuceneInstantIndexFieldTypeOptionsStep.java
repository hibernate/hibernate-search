/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.Instant;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneInstantFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class LuceneInstantIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneInstantIndexFieldTypeOptionsStep, Instant> {

	LuceneInstantIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class, DefaultParseConverters.INSTANT );
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Instant, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Instant indexNullAsValue) {
		return new LuceneInstantFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	protected LuceneInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
