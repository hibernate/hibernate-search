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

import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManager;
import org.hibernate.search.engine.logging.spi.FailureContext;
import org.hibernate.search.engine.logging.spi.FailureContextFormatter;
import org.hibernate.search.engine.logging.spi.SearchExceptionWithContext;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import org.apache.lucene.search.Query;

@MessageLogger(projectCode = "HSEARCH-LUCENE")
public interface Log extends BasicLogger {

	// -----------------------
	// Pre-existing messages
	// -----------------------

	@LogMessage(level = Level.WARN)
	@Message(id = 35, value = "Could not close resource.")
	void couldNotCloseResource(@Cause Exception e);

	@LogMessage(level = Level.WARN)
	@Message(id = 55, value = "Unable to close the index reader. %1$s")
	void unableToCloseIndexReader(@FormatWith(FailureContextFormatter.class) FailureContext context, @Cause Exception e);

	@Message(id = 284, value = "An IOException happened while opening multiple indexes." )
	SearchExceptionWithContext ioExceptionOnMultiReaderRefresh(@Param FailureContext context, @Cause IOException e);

	@Message(id = 320, value = "Could not normalize value for field '%1$s'.")
	SearchException couldNotNormalizeField(String absoluteFieldPath, @Cause Exception cause);

	@LogMessage(level = Level.WARN)
	@Message(id = 321, value = "The analysis of field '%1$s' produced multiple tokens. Tokenization or term generation"
			+ " (synonyms) should not be used on sortable fields or range queries. Only the first token will be considered.")
	void multipleTermsDetectedDuringNormalization(String absoluteFieldPath);

	// -----------------------
	// New messages
	// -----------------------

	@Message(id = 500, value = "Unknown field '%1$s'." )
	SearchExceptionWithContext unknownFieldForSearch(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 501, value = "Root directory '%1$s' exists but is not a writable directory.")
	SearchExceptionWithContext localDirectoryBackendRootDirectoryNotWritableDirectory(Path rootDirectory,
			@Param FailureContext context);

	@Message(id = 502, value = "Unable to create root directory '%1$s'.")
	SearchExceptionWithContext unableToCreateRootDirectoryForLocalDirectoryBackend(Path rootDirectory,
			@Param FailureContext context, @Cause Exception e);

	@Message(id = 503, value = "Undefined Lucene directory provider.")
	SearchExceptionWithContext undefinedLuceneDirectoryProvider(@Param FailureContext context);

	@Message(id = 504, value = "Unrecognized Lucene directory provider '%1$s'.")
	SearchExceptionWithContext unrecognizedLuceneDirectoryProvider(String directoryProvider, @Param FailureContext context);

	@Message(id = 505, value = "The Lucene extension can only be applied to objects"
			+ " derived from the Lucene backend. Was applied to '%1$s' instead." )
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = 506, value = "An analyzer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchExceptionWithContext cannotUseAnalyzerOnFieldType(String relativeFieldName, @Param FailureContext context);

	@Message(id = 507, value = "A normalizer was set on field '%1$s', but fields of this type cannot be analyzed." )
	SearchExceptionWithContext cannotUseNormalizerOnFieldType(String relativeFieldName, @Param FailureContext context);

	@Message(id = 510, value = "A Lucene query cannot include search predicates built using a non-Lucene search target."
			+ " Given predicate was: '%1$s'" )
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = 511, value = "Multiple conflicting types for field '%1$s': '%2$s' vs. '%3$s'." )
	SearchExceptionWithContext conflictingFieldTypesForSearch(String absoluteFieldPath,
			LuceneIndexSchemaFieldNode<?> schemaNode1, LuceneIndexSchemaFieldNode<?> schemaNode2,
			@Param FailureContext context);

	@Message(id = 512, value = "Field '%1$s' is not an object field." )
	SearchExceptionWithContext nonObjectFieldForNestedQuery(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 513, value = "Object field '%1$s' is not stored as nested." )
	SearchExceptionWithContext nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 514, value = "A Lucene query cannot include search sorts built using a non-Lucene search target."
			+ " Given sort was: '%1$s'" )
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = 515, value = "Unable to create the IndexWriter." )
	SearchExceptionWithContext unableToCreateIndexWriter(@Param FailureContext context, @Cause Exception e);

	@Message(id = 516, value = "Unable to index entry '%2$s' with tenant identifier '%1$s'." )
	SearchExceptionWithContext unableToIndexEntry(String tenantId, String id,
			@Param FailureContext context, @Cause Exception e);

	@Message(id = 517, value = "Unable to delete entry '%2$s' with tenant identifier '%1$s'." )
	SearchExceptionWithContext unableToDeleteEntryFromIndex(String tenantId, String id,
			@Param FailureContext context, @Cause Exception e);

	@Message(id = 518, value = "Unable to flush." )
	SearchExceptionWithContext unableToFlushIndex(@Param FailureContext context, @Cause Exception e);

	@Message(id = 519, value = "Unable to commit." )
	SearchExceptionWithContext unableToCommitIndex(@Param FailureContext context, @Cause Exception e);

	@Message(id = 520, value = "Index directory '%1$s' exists but is not a writable directory.")
	SearchExceptionWithContext localDirectoryIndexRootDirectoryNotWritableDirectory(Path indexDirectory,
			@Param FailureContext context);

	@Message(id = 521, value = "Unable to create index root directory '%1$s'.")
	SearchExceptionWithContext unableToCreateIndexRootDirectoryForLocalDirectoryBackend(Path indexDirectory,
			@Param FailureContext context, @Cause Exception e);

	@Message(id = 522, value = "Could not open an index reader.")
	SearchExceptionWithContext unableToCreateIndexReader(@Param FailureContext context, @Cause Exception e);

	@Message(id = 524, value = "A search query cannot target both a Lucene index and other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchExceptionWithContext cannotMixLuceneSearchTargetWithOtherType(IndexSearchTargetBuilder firstTarget,
			LuceneIndexManager otherTarget, @Param FailureContext context);

	@Message(id = 525, value = "A search query cannot target multiple Lucene backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchExceptionWithContext cannotMixLuceneSearchTargetWithOtherBackend(IndexSearchTargetBuilder firstTarget,
			LuceneIndexManager otherTarget, @Param FailureContext context);

	@Message(id = 526, value = "Unknown projections %1$s." )
	SearchExceptionWithContext unknownProjectionForSearch(Collection<String> projections, @Param FailureContext context);

	@Message(id = 527, value = "An IOException happened while executing the query '%1$s'." )
	SearchExceptionWithContext ioExceptionOnQueryExecution(Query luceneQuery, @Param FailureContext context, @Cause IOException e);

	@Message(id = 529, value = "Index '%1$s' requires multi-tenancy but the backend does not support it in its current configuration.")
	SearchExceptionWithContext multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param FailureContext context);

	@Message(id = 530, value = "Unknown multi-tenancy strategy '%1$s'.")
	SearchException unknownMultiTenancyStrategyConfiguration(String multiTenancyStrategy);

	@Message(id = 531, value = "Tenant identifier '%1$s' is provided, but multi-tenancy is disabled for this backend.")
	SearchExceptionWithContext tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param FailureContext context);

	@Message(id = 532, value = "Backend has multi-tenancy enabled, but no tenant identifier is provided.")
	SearchExceptionWithContext multiTenancyEnabledButNoTenantIdProvided(@Param FailureContext context);

	@Message(id = 533, value = "Attempt to unwrap a Lucene backend to %1$s,"
			+ " but this backend can only be unwrapped to %2$s." )
	SearchExceptionWithContext backendUnwrappingWithUnknownType(Class<?> requestedClass, Class<?> actualClass,
			@Param FailureContext context);

	@Message(id = 534, value = "The index schema node '%1$s' was added twice."
			+ " Multiple bridges may be trying to access the same index field, "
			+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
			+ " or you may have declared multiple conflicting mappings."
			+ " In any case, there is something wrong with your mapping and you should fix it." )
	SearchExceptionWithContext indexSchemaNodeNameConflict(String relativeFieldName,
			@Param FailureContext context);

	@Message(id = 537, value = "Range predicates are not supported by the GeoPoint field type, use spatial predicates instead.")
	SearchExceptionWithContext rangePredicatesNotSupportedByGeoPoint(@Param FailureContext context);

	@Message(id = 538, value = "Match predicates are not supported by the GeoPoint field type, use spatial predicates instead.")
	SearchExceptionWithContext matchPredicatesNotSupportedByGeoPoint(@Param FailureContext context);

	@Message(id = 539, value = "Invalid parent object for this field accessor; expected path '%1$s', got '%2$s'.")
	SearchException invalidParentDocumentObjectState(String expectedPath, String actualPath);

	@Message(id = 540, value = "Spatial predicates are not supported by this field's type.")
	SearchExceptionWithContext spatialPredicatesNotSupportedByFieldType(@Param FailureContext context);

	@Message(id = 541, value = "Distance related operations are not supported by this field's type.")
	SearchExceptionWithContext distanceOperationsNotSupportedByFieldType(@Param FailureContext context);

	@Message(id = 542, value = "Traditional sorting operations are not supported by the GeoPoint field type, use distance sorting instead.")
	SearchExceptionWithContext traditionalSortNotSupportedByGeoPoint(@Param FailureContext context);

	@Message(id = 543, value = "Descending order is not supported for distance sort.")
	SearchExceptionWithContext descendingOrderNotSupportedByDistanceSort(@Param FailureContext context);

	@Message(id = 544, value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
			+ " expected a number between 1 and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int minimum, int totalShouldClauseNumber);

	@Message(id = 545, value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ignoreConstraintCeiling);

	@Message(id = 546, value = "Native fields do not support defining predicates with the DSL: use the Lucene extension and a native query.")
	SearchExceptionWithContext unsupportedDSLPredicates(@Param FailureContext context);

	@Message(id = 547, value = "Native fields do not support defining sorts with the DSL: use the Lucene extension and a native sort.")
	SearchExceptionWithContext unsupportedDSLSorts(@Param FailureContext context);

	@Message(id = 548, value = "This native field does not support projection.")
	SearchExceptionWithContext unsupportedProjection(@Param FailureContext context);

	@Message(id = 549, value = "Invalid field path; expected path '%1$s', got '%2$s'.")
	SearchException invalidFieldPath(String expectedPath, String actualPath);

}
