/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneByteFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class LuceneByteIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneByteIndexFieldTypeOptionsStep, Byte> {

	LuceneByteIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Byte.class, DefaultParseConverters.BYTE );
	}

	@Override
	protected LuceneByteIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Byte, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Byte indexNullAsValue) {
		return new LuceneByteFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
