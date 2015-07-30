/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LogCategory;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
* @author Sanne Grinovero
*/
public final class LazyQueryState implements Closeable {

	private static final Log log = LoggerFactory.make();
	private static final Log QUERY_LOG = LoggerFactory.make( LogCategory.QUERY );

	private final Query userQuery;
	private final IndexSearcher searcher;
	private final boolean fieldSortDoTrackScores;
	private final boolean fieldSortDoMaxScore;
	private final ExtendedSearchIntegrator extendedIntegrator;
	private final Set<Class<?>> targetedTypes;

	private Query rewrittenQuery;

	public LazyQueryState(Query userQuery,
			IndexReader reader,
			Similarity searcherSimilarity,
			ExtendedSearchIntegrator extendedIntegrator,
			Set<Class<?>> targetedTypes,
			boolean fieldSortDoTrackScores,
			boolean fieldSortDoMaxScore) {
		this.userQuery = userQuery;
		this.fieldSortDoTrackScores = fieldSortDoTrackScores;
		this.fieldSortDoMaxScore = fieldSortDoMaxScore;
		this.searcher = new IndexSearcher( reader );
		this.searcher.setSimilarity( searcherSimilarity );
		this.extendedIntegrator = extendedIntegrator;
		this.targetedTypes = targetedTypes;
	}

	public boolean isFieldSortDoTrackScores() {
		return fieldSortDoTrackScores;
	}

	public boolean isFieldSortDoMaxScore() {
		return fieldSortDoMaxScore;
	}

	public Explanation explain(int documentId) throws IOException {
		return searcher.explain( rewrittenQuery(), documentId );
	}

	public Explanation explain(Query filteredQuery, int documentId) throws IOException {
		return searcher.explain( filteredQuery, documentId );
	}

	public Document doc(final int docId) throws IOException {
		return searcher.doc( docId );
	}

	public void doc(final int docId, final StoredFieldVisitor fieldVisitor) throws IOException {
		searcher.doc( docId, fieldVisitor );
	}

	public int maxDoc() {
		//pointless to try caching this one
		return searcher.getIndexReader().maxDoc();
	}

	public void search(final Filter filter, final Collector collector) throws IOException {
		validateQuery();
		QUERY_LOG.executingLuceneQuery( userQuery );
		searcher.search( rewrittenQuery(), filter, collector );
	}

	public IndexReader getIndexReader() {
		return searcher.getIndexReader();
	}

	@Override
	public void close() {
		final IndexReader indexReader = searcher.getIndexReader();
		try {
			MultiReaderFactory.closeReader( indexReader );
		}
		catch (SearchException e) {
			log.unableToCloseSearcherDuringQuery( userQuery.toString(), e );
		}
	}

	public String describeQuery() {
		return userQuery.toString();
	}

	private void validateQuery() {
		// HSEARCH-763 we make an attempt to validate whether the queries target appropriate fields, eg
		// numeric fields should not be part of a text based query, instead a NumericRangeQuery should be used.
		// Validation is based on the original user specified query, since the rewritten query amongst other things
		// rewrites queries to use filters
		Set<String> stringEncodedFieldNames = new HashSet<>();
		Set<String> numericEncodedFieldNames = new HashSet<>();
		for ( Class<?> targetedType : targetedTypes ) {
			IndexedTypeDescriptor indexedTypeDescriptor = extendedIntegrator.getIndexedTypeDescriptor(
					targetedType
			);
			// get the field contributions from the type (class bridges)
			for ( FieldDescriptor fieldDescriptor : indexedTypeDescriptor.getIndexedFields() ) {
				putInFieldTypeBucket( stringEncodedFieldNames, numericEncodedFieldNames, fieldDescriptor );
			}
			// get the field contributions from the properties
			for ( PropertyDescriptor propertyDescriptor : indexedTypeDescriptor.getIndexedProperties() ) {
				for ( FieldDescriptor fieldDescriptor : propertyDescriptor.getIndexedFields() ) {
					putInFieldTypeBucket( stringEncodedFieldNames, numericEncodedFieldNames, fieldDescriptor );
				}
			}
		}

		FieldNameCollector.FieldCollection fieldCollection = FieldNameCollector.extractFieldNames( userQuery );

		if ( !Collections.disjoint( stringEncodedFieldNames, fieldCollection.getNumericFieldNames() ) ) {
			Set<String> invalidFields = new HashSet<>();
			for ( String numericField : fieldCollection.getNumericFieldNames() ) {
				if ( stringEncodedFieldNames.contains( numericField ) ) {
					invalidFields.add( numericField );
				}
			}
			throw log.stringEncodedFieldsAreTargetedWithNumericQuery( userQuery.toString(), StringHelper.join( invalidFields, "," ) );
		}

		if ( !Collections.disjoint( numericEncodedFieldNames, fieldCollection.getStringFieldNames() ) ) {
			Set<String> invalidFields = new HashSet<>();
			for ( String stringField : fieldCollection.getStringFieldNames() ) {
				if ( numericEncodedFieldNames.contains( stringField ) ) {
					invalidFields.add( stringField );
				}
			}
			throw log.numericEncodedFieldsAreTargetedWithStringQuery( userQuery.toString(), StringHelper.join( invalidFields, "," ) );
		}
	}

	private void putInFieldTypeBucket(Set<String> stringEncodedFieldNames,
			Set<String> numericEncodedFieldNames,
			FieldDescriptor fieldDescriptor) {
		if ( FieldDescriptor.Type.NUMERIC.equals( fieldDescriptor.getType() ) ) {
			// If the property allows to index a string based null token, a non numeric query is ok
			// We are currently skipping this case completely from the validation. In a full on implementation we would
			// need to take term values into account and in the case of a string query only allow the null token.
			// For now we cast a net which is potentially too wide (HF)
			if ( !fieldDescriptor.indexNull() ) {
				numericEncodedFieldNames.add( fieldDescriptor.getName() );
			}
		}
		else {
			stringEncodedFieldNames.add( fieldDescriptor.getName() );
		}
	}

	private Query rewrittenQuery() throws IOException {
		if ( rewrittenQuery == null ) {
			rewrittenQuery = userQuery.rewrite( searcher.getIndexReader() );
		}
		return rewrittenQuery;
	}
}
