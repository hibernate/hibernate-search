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
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManager;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.logging.spi.FailureContextElement;
import org.hibernate.search.engine.logging.spi.SearchExceptionWithContext;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.util.SearchException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@MessageLogger(projectCode = "HSEARCH-LUCENE")
public interface Log extends BasicLogger {

	// -----------------------
	// Pre-existing messages
	// -----------------------

	@LogMessage(level = Level.WARN)
	@Message(id = 35, value = "Could not close resource.")
	void couldNotCloseResource(@Cause Exception e);

	@LogMessage(level = Level.WARN)
	@Message(id = 55, value = "Unable to close the index reader for index '%2$s' of backend '%1$s'.")
	void unableToCloseIndexReader(BackendImplementor<?> backend, String indexName, @Cause Exception e);

	@Message(id = 284, value = "An IOException happened while opening multiple indexes %1$s." )
	SearchException ioExceptionOnMultiReaderRefresh(Collection<String> indexNames, @Cause IOException e);

	@Message(id = 320, value = "Could not normalize value for field '%1$s'.")
	SearchException couldNotNormalizeField(String absoluteFieldPath, @Cause Exception cause);

	@LogMessage(level = Level.WARN)
	@Message(id = 321, value = "The analysis of field '%1$s' produced multiple tokens. Tokenization or term generation"
			+ " (synonyms) should not be used on sortable fields or range queries. Only the first token will be considered.")
	void multipleTermsDetectedDuringNormalization(String absoluteFieldPath);

	// -----------------------
	// New messages
	// -----------------------

	@Message(id = 500, value = "Unknown field '%1$s' in indexes %2$s." )
	SearchException unknownFieldForSearch(String absoluteFieldPath, Collection<String> indexNames);

	@Message(id = 501, value = "Root directory '%2$s' of backend '%1$s' exists but is not a writable directory.")
	SearchException localDirectoryBackendRootDirectoryNotWritableDirectory(String backendName, Path rootDirectory);

	@Message(id = 502, value = "Unable to create root directory '%2$s' for backend '%1$s'.")
	SearchException unableToCreateRootDirectoryForLocalDirectoryBackend(String backendName, Path rootDirectory, @Cause Exception e);

	@Message(id = 503, value = "Undefined Lucene directory provider for backend '%1$s'.")
	SearchException undefinedLuceneDirectoryProvider(String backendName);

	@Message(id = 504, value = "Unrecognized Lucene directory provider '%2$s' for backend '%1$s'.")
	SearchException unrecognizedLuceneDirectoryProvider(String backendName, String backendType);

	@Message(id = 505, value = "The Lucene extension can only be applied to objects"
			+ " derived from the Lucene backend. Was applied to '%1$s' instead." )
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = 506, value = "An analyzer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchException cannotUseAnalyzerOnFieldType(String relativeFieldName);

	@Message(id = 507, value = "A normalizer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchException cannotUseNormalizerOnFieldType(String relativeFieldName);

	@Message(id = 510, value = "A Lucene query cannot include search predicates built using a non-Lucene search target."
			+ " Given predicate was: '%1$s'" )
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = 511, value = "Multiple conflicting types for field '%1$s': '%2$s' in index '%3$s', but '%4$s' in index '%5$s'." )
	SearchException conflictingFieldTypesForSearch(String absoluteFieldPath,
			LuceneIndexSchemaFieldNode<?> schemaNode1, String indexName1,
			LuceneIndexSchemaFieldNode<?> schemaNode2, String indexName2);

	@Message(id = 512, value = "Field '%2$s' is not an object field in index '%1$s'." )
	SearchException nonObjectFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 513, value = "Object field '%2$s' is not stored as nested in index '%1$s'." )
	SearchException nonNestedFieldForNestedQuery(String indexName, String absoluteFieldPath);

	@Message(id = 514, value = "A Lucene query cannot include search sorts built using a non-Lucene search target."
			+ " Given sort was: '%1$s'" )
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = 515, value = "Unable to create the IndexWriter for backend '%1$s', index '%2$s'." )
	SearchException unableToCreateIndexWriter(BackendImplementor<?> backend, String indexName, @Cause Exception e);

	@Message(id = 516, value = "Unable to index entry '%3$s' for index '%1$s' and tenant identifier '%2$s'." )
	SearchException unableToIndexEntry(String indexName, String tenantId, String id, @Cause Exception e);

	@Message(id = 517, value = "Unable to delete entry '%3$s' from index '%1$s' and tenant identifier '%2$s'." )
	SearchException unableToDeleteEntryFromIndex(String indexName, String tenantId, String id, @Cause Exception e);

	@Message(id = 518, value = "Unable to flush index '%1$s'." )
	SearchException unableToFlushIndex(String indexName, @Cause Exception e);

	@Message(id = 519, value = "Unable to commit index '%1$s'." )
	SearchException unableToCommitIndex(String indexName, @Cause Exception e);

	@Message(id = 520, value = "Index directory '%2$s' of backend '%1$s' exists but is not a writable directory.")
	SearchException localDirectoryIndexRootDirectoryNotWritableDirectory(BackendImplementor<?> backend, Path indexDirectory);

	@Message(id = 521, value = "Unable to create index root directory '%2$s' for backend '%1$s'.")
	SearchException unableToCreateIndexRootDirectoryForLocalDirectoryBackend(BackendImplementor<?> backend, Path indexDirectory, @Cause Exception e);

	@Message(id = 522, value = "Could not open an index reader for index '%2$s' of backend '%1$s'.")
	SearchException unableToCreateIndexReader(BackendImplementor<?> backend, String indexName, @Cause Exception e);

	@Message(id = 524, value = "A search query cannot target both a Lucene index and other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixLuceneSearchTargetWithOtherType(IndexSearchTargetBuilder firstTarget, LuceneIndexManager otherTarget);

	@Message(id = 525, value = "A search query cannot target multiple Lucene backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchException cannotMixLuceneSearchTargetWithOtherBackend(IndexSearchTargetBuilder firstTarget, LuceneIndexManager otherTarget);

	@Message(id = 526, value = "Unknown projections %1$s in indexes %2$s." )
	SearchException unknownProjectionForSearch(Collection<String> projections, Collection<String> indexNames);

	@Message(id = 527, value = "An IOException happened while executing the query '%1$s' on indexes %2$s." )
	SearchException ioExceptionOnQueryExecution(Query luceneQuery, Collection<String> indexNames, @Cause IOException e);

	@Message(id = 528, value = "Cannot define field '%1$s' as sortable: fields of this type cannot be sortable." )
	SearchException cannotUseSortableOnFieldType(String relativeFieldName);

	@Message(id = 529, value = "Index '%2$s' requires multi-tenancy but backend '%1$s' does not support it in its current configuration.")
	SearchException multiTenancyRequiredButNotSupportedByBackend(BackendImplementor<?> backend, String indexName);

	@Message(id = 530, value = "Unknown multi-tenancy strategy '%1$s'.")
	SearchException unknownMultiTenancyStrategyConfiguration(String multiTenancyStrategy);

	@Message(id = 531, value = "Tenant identifier '%2$s' is provided, but multi-tenancy is disabled for the backend '%1$s'.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(BackendImplementor<?> backend, String tenantId);

	@Message(id = 532, value = "Backend '%1$s' has multi-tenancy enabled, but no tenant identifier is provided.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(BackendImplementor<?> backend);

	@Message(id = 533, value = "Attempt to unwrap a Lucene backend to %1$s,"
			+ " but this backend can only be unwrapped to %2$s." )
	SearchException backendUnwrappingWithUnknownType(Class<?> requestedClass, Class<?> actualClass);

	@Message(id = 534, value = "The index schema node '%1$s' was added twice."
			+ " Multiple bridges may be trying to access the same index field, "
			+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
			+ " or you may have declared multiple conflicting mappings."
			+ " In any case, there is something wrong with your mapping and you should fix it." )
	SearchExceptionWithContext indexSchemaNodeNameConflict(String relativeFieldName,
			@Param List<FailureContextElement> context);

	@Message(id = 537, value = "Range predicates are not supported by the GeoPoint type of field '%1$s', use spatial predicates instead.")
	SearchException rangePredicatesNotSupportedByGeoPoint(String absoluteFieldPath);

	@Message(id = 538, value = "Match predicates are not supported by the GeoPoint type of field '%1$s', use spatial predicates instead.")
	SearchException matchPredicatesNotSupportedByGeoPoint(String absoluteFieldPath);

	@Message(id = 539, value = "Invalid parent object for this field accessor; expected path '%1$s', got '%2$s'.")
	SearchException invalidParentDocumentObjectState(String expectedPath, String actualPath);

	@Message(id = 540, value = "Spatial predicates are not supported by the type of field '%1$s'.")
	SearchException spatialPredicatesNotSupportedByFieldType(String absoluteFieldPath);

	@Message(id = 541, value = "Distance related operations are not supported by the type of field '%1$s'.")
	SearchException distanceOperationsNotSupportedByFieldType(String absoluteFieldPath);

	@Message(id = 542, value = "Traditional sorting operations are not supported by the GeoPoint type of field '%1$s', use distance sorting instead.")
	SearchException traditionalSortNotSupportedByGeoPoint(String absoluteFieldPath);

	@Message(id = 543, value = "Descending order is not supported by distance sort for field '%1$s'.")
	SearchException descendingOrderNotSupportedByDistanceSort(String absoluteFieldPath);

	@Message(id = 544, value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
			+ " expected a number between 1 and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int minimum, int totalShouldClauseNumber);

	@Message(id = 545, value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ignoreConstraintCeiling);

	@Message(id = 546, value = "Field '%1$s' does not support defining predicates with the DSL: use the Lucene extension and a native query.")
	SearchException unsupportedDSLPredicates(String absoluteFieldPath);

	@Message(id = 547, value = "Field '%1$s' does not support defining sorts with the DSL: use the Lucene extension and a native sort.")
	SearchException unsupportedDSLSorts(String absoluteFieldPath);

	@Message(id = 548, value = "Field '%1$s' does not support projection.")
	SearchException unsupportedProjection(String absoluteFieldPath);

	@Message(id = 549, value = "Invalid field path; expected path '%1$s', got '%2$s'.")
	SearchException invalidFieldPath(String expectedPath, String actualPath);
}
