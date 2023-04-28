/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.document.impl.LuceneIdReader;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.backend.common.DocumentReference;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReaderContext;

public abstract class DocumentReferenceValues<R> implements Values<R> {

	public static DocumentReferenceValues<DocumentReference> simple(ProjectionExtractContext context) {
		return new DocumentReferenceValues<DocumentReference>( context.idReader(), context.collectorExecutionContext() ) {
			@Override
			protected DocumentReference toReference(String typeName, String identifier) {
				return new LuceneDocumentReference( typeName, identifier );
			}
		};
	}

	private final LuceneIdReader idReader;
	private final IndexReaderMetadataResolver metadataResolver;

	private String currentLeafMappedTypeName;
	private BinaryDocValues currentLeafIdDocValues;

	protected DocumentReferenceValues(LuceneIdReader idReader, CollectorExecutionContext executionContext) {
		this.idReader = idReader;
		this.metadataResolver = executionContext.getMetadataResolver();
	}

	@Override
	public final void context(LeafReaderContext context) throws IOException {
		this.currentLeafMappedTypeName = metadataResolver.resolveMappedTypeName( context );
		this.currentLeafIdDocValues = idReader.idDocValues( context.reader() );
	}

	@Override
	public final R get(int doc) throws IOException {
		currentLeafIdDocValues.advance( doc );
		return toReference(
				currentLeafMappedTypeName,
				currentLeafIdDocValues.binaryValue().utf8ToString()
		);
	}

	protected abstract R toReference(String typeName, String identifier);

}
