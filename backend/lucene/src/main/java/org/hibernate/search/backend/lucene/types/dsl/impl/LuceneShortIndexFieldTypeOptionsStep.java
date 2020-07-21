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
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneShortFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneShortIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneShortIndexFieldTypeOptionsStep, Short> {

	LuceneShortIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Short.class );
	}

	@Override
	protected LuceneShortIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Short, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Short indexNullAsValue) {
		return new LuceneShortFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
