/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class SearchProjectionExecutionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FromDocumentFieldValueConvertContext fromDocumentFieldValueConvertContext;

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	public SearchProjectionExecutionContext(SessionContextImplementor sessionContext,
			IndexSearcher indexSearcher,
			Query luceneQuery) {
		this.fromDocumentFieldValueConvertContext = new FromDocumentFieldValueConvertContextImpl( sessionContext );
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
	}

	FromDocumentFieldValueConvertContext getFromDocumentFieldValueConvertContext() {
		return fromDocumentFieldValueConvertContext;
	}

	public Explanation explain(int docId) {
		try {
			return indexSearcher.explain( luceneQuery, docId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnExplain( e );
		}
	}
}
