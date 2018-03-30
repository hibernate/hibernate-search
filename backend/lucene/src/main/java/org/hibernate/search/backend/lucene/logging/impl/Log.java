/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.logging.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManager;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.util.SearchException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-LUCENE")
public interface Log extends BasicLogger {

	@Message(id = 4, value = "Unknown field '%1$s' in indexes %2$s." )
	SearchException unknownFieldForSearch(String absoluteFieldPath, Collection<String> indexNames);

	@Message(id = 5, value = "Root directory '%2$s' of backend '%1$s' exists but is not a writable directory.")
	SearchException localDirectoryBackendRootDirectoryNotWritableDirectory(String backendName, Path rootDirectory);

	@Message(id = 6, value = "Unable to create root directory '%2$s' for backend '%1$s'.")
	SearchException unableToCreateRootDirectoryForLocalDirectoryBackend(String backendName, Path rootDirectory, @Cause Exception e);

	@Message(id = 7, value = "Undefined Lucene directory provider for backend '%1$s'.")
	SearchException undefinedLuceneDirectoryProvider(String backendName);

	@Message(id = 8, value = "Unrecognized Lucene directory provider '%2$s' for backend '%1$s'.")
	SearchException unrecognizedLuceneDirectoryProvider(String backendName, String backendType);

	@Message(id = 9, value = "The Lucene extension can only be applied to objects"
			+ " derived from the Lucene backend. Was applied to '%1$s' instead." )
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = 12, value = "An analyzer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchException cannotUseAnalyzerOnFieldType(String fieldName);

	@Message(id = 13, value = "A normalizer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchException cannotUseNormalizerOnFieldType(String fieldName);

	@Message(id = 14, value = "Cannot use an analyzer on field '%1$s' because it is sortable." )
	SearchException cannotUseAnalyzerOnSortableField(String fieldName);

	@Message(id = 15, value = "Could not analyze sortable field '%1$s'.")
	SearchException couldNotAnalyzeSortableField(String fieldName, @Cause Exception cause);

	@LogMessage(level = Level.WARN)
	@Message(id = 16, value = "The analysis of field '%1$s' produced multiple tokens. Tokenization or term generation"
			+ " (synonyms) should not be used on sortable fields. Only the first token will be indexed.")
	void multipleTermsInAnalyzedSortableField(String fieldName);

	@Message(id = 17, value = "A Lucene query cannot include search predicates built using a non-Lucene search target."
			+ " Given predicate was: '%1$s'" )
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = 18, value = "Multiple conflicting types for field '%1$s': '%2$s' in index '%3$s', but '%4$s' in index '%5$s'." )
	SearchException conflictingFieldTypesForSearch(String absoluteFieldPath,
			LuceneIndexSchemaFieldNode<?> schemaNode1, String indexName1,
			LuceneIndexSchemaFieldNode<?> schemaNode2, String indexName2);

	@Message(id = 19, value = "Field '%2$s' is not an object field in index '%1$s'." )
	SearchException nonObjectFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 20, value = "Object field '%2$s' is not stored as nested in index '%1$s'." )
	SearchException nonNestedFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 21, value = "A Lucene query cannot include search sorts built using a non-Lucene search target."
			+ " Given sort was: '%1$s'" )
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = 22, value = "Unable to create the IndexWriter for backend '%1$s', index '%2$s' and path '%3$s'." )
	SearchException unableToCreateIndexWriter(String backendName, String indexName, Path directoryPath, @Cause Exception e);

	@Message(id = 23, value = "Unable to index entry '%3$s' for index '%1$s' and tenantId '%2$s'." )
	SearchException unableToIndexEntry(String indexName, String tenantId, String id, @Cause Exception e);

	@Message(id = 24, value = "Unable to delete entry '%3$s' from index '%1$s' and tenantId '%2$s'." )
	SearchException unableToDeleteEntryFromIndex(String indexName, String tenantId, String id, @Cause Exception e);

	@Message(id = 25, value = "Unable to flush index '%1$s'." )
	SearchException unableToFlushIndex(String indexName, @Cause Exception e);

	@Message(id = 26, value = "Unable to commit index '%1$s'." )
	SearchException unableToCommitIndex(String indexName, @Cause Exception e);

	@Message(id = 27, value = "Index directory '%2$s' of backend '%1$s' exists but is not a writable directory.")
	SearchException localDirectoryIndexRootDirectoryNotWritableDirectory(String backendName, Path indexDirectory);

	@Message(id = 28, value = "Unable to create index root directory '%2$s' for backend '%1$s'.")
	SearchException unableToCreateIndexRootDirectoryForLocalDirectoryBackend(String backendName, Path indexDirectory, @Cause Exception e);

	@Message(id = 29, value = "Could not open an index reader for index '%2$s' of backend '%1$s'.")
	SearchException unableToCreateIndexReader(String backendName, String indexName, @Cause Exception e);

	@LogMessage(level = Level.WARN)
	@Message(id = 30, value = "Unable to close the index reader for index '%2$s' of backend '%1$s'.")
	void unableToCloseIndexReader(String backendName, String indexName, @Cause Exception e);

	@Message(id = 31, value = "A search query cannot target both a Lucene index and other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixLuceneSearchTargetWithOtherType(IndexSearchTargetBuilder firstTarget, LuceneIndexManager otherTarget);

	@Message(id = 32, value = "A search query cannot target multiple Lucene backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixLuceneSearchTargetWithOtherBackend(IndexSearchTargetBuilder firstTarget, LuceneIndexManager otherTarget);

	@Message(id = 33, value = "An IOException happened while opening multiple indexes %1$s." )
	SearchException ioExceptionOnMultiReaderRefresh(Collection<String> indexNames, @Cause IOException e);

	@LogMessage(level = Level.WARN)
	@Message(id = 34, value = "Could not close resource.")
	void couldNotCloseResource(@Cause Exception e);

	@Message(id = 35, value = "Unknown projections %1$s in indexes %2$s." )
	SearchException unknownProjectionForSearch(Collection<String> projections, Collection<String> indexNames);

	@Message(id = 36, value = "An IOException happened while executing the query '%1$s' on indexes %2$s." )
	SearchException ioExceptionOnQueryExecution(Query luceneQuery, Collection<String> indexNames, @Cause IOException e);

	@Message(id = 37, value = "Cannot set sortable for field '%1$s': fields of this type cannot be sortable." )
	SearchException cannotUseSortableOnFieldType(String fieldName);
}
