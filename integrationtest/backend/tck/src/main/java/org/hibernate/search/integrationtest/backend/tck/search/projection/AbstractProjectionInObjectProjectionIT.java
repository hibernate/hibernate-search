/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

public abstract class AbstractProjectionInObjectProjectionIT<F, P, V extends AbstractProjectionTestValues<F, P>> {

	private static final String FULL_DOCUMENT_ID = "fullDocId";
	private static final String SINGLE_VALUED_DOCUMENT_ID = "singleValuedDocId";
	private static final String LEVEL1_SINGLE_OBJECT_DOCUMENT_ID = "level1SingleObjectDocId";
	private static final String LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID = "level1SingleEmptyObjectDocId";
	private static final String LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID = "level1SingleNullObjectDocId";
	private static final String LEVEL1_NO_OBJECT_DOCUMENT_ID = "level1NoObjectDocId";
	private static final String LEVEL2_SINGLE_OBJECT_DOCUMENT_ID = "level2SingleObjectDocId";
	private static final String LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID = "level2SingleEmptyObjectDocId";
	private static final String LEVEL2_NO_OBJECT_DOCUMENT_ID = "level2NoObjectDocId";

	private static final String MISSING_LEVEL1_DOCUMENT_ID = "missingLevel1DocId";
	private static final String MISSING_LEVEL1_SINGLE_VALUED_FIELD_DOCUMENT_ID = "missingLevel1SVFieldDocId";
	private static final String MISSING_LEVEL2_DOCUMENT_ID = "missingLevel2DocId";
	private static final String MISSING_LEVEL2_SINGLE_VALUED_FIELD_DOCUMENT_ID = "missingLevel2SVFieldDocId";

	private final SimpleMappedIndex<IndexBinding> mainIndex;
	private final SimpleMappedIndex<MissingLevel1IndexBinding> missingLevel1Index;
	private final SimpleMappedIndex<MissingLevel1SingleValuedFieldIndexBinding> missingLevel1SingleValuedFieldIndex;
	private final SimpleMappedIndex<MissingLevel2IndexBinding> missingLevel2Index;
	private final SimpleMappedIndex<MissingLevel2SingleValuedFieldIndexBinding> missingLevel2SingleValuedFieldIndex;
	protected final DataSet<F, P, V> dataSet;
	private final RecursiveComparisonConfiguration recursiveComparisonConfig;

	public AbstractProjectionInObjectProjectionIT(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingLevel1IndexBinding> missingLevel1Index,
			SimpleMappedIndex<MissingLevel1SingleValuedFieldIndexBinding> missingLevel1SingleValuedFieldIndex,
			SimpleMappedIndex<MissingLevel2IndexBinding> missingLevel2Index,
			SimpleMappedIndex<MissingLevel2SingleValuedFieldIndexBinding> missingLevel2SingleValuedFieldIndex,
			DataSet<F, P, V> dataSet) {
		this.mainIndex = mainIndex;
		this.missingLevel1Index = missingLevel1Index;
		this.missingLevel1SingleValuedFieldIndex = missingLevel1SingleValuedFieldIndex;
		this.missingLevel2Index = missingLevel2Index;
		this.missingLevel2SingleValuedFieldIndex = missingLevel2SingleValuedFieldIndex;
		this.dataSet = dataSet;
		this.recursiveComparisonConfig = configureRecursiveComparison( RecursiveComparisonConfiguration.builder() )
				.build();
	}

	protected RecursiveComparisonConfiguration.Builder configureRecursiveComparison(RecursiveComparisonConfiguration.Builder builder) {
		return builder;
	}

	@Test
	public void objectOnLevel1AndObjectOnLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( level1Path() )
										.from(
												singleValuedProjection( f, level1SingleValuedFieldPath() ),
												multiValuedProjection( f, level1MultiValuedFieldPath() ),
												f.object( level2Path() )
														.from(
																singleValuedProjection( f, level2SingleValuedFieldPath() ),
																multiValuedProjection( f, level2MultiValuedFieldPath() )
														)
														.as( ObjectDto::new )
														.multi()
										)
										.as( Level1ObjectDto::new )
										.multi()
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, listMaybeWithNull(
								null,
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										listMaybeWithNull(
												null,
												new ObjectDto<>(
														dataSet.values.projectedValue( 3 ),
														dataSet.values.projectedValues( 4, 5 )
												),
												new ObjectDto<>(
														dataSet.values.projectedValue( 6 ),
														dataSet.values.projectedValues( 7, 8 )
												)
										)
								),
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 9 ),
										dataSet.values.projectedValues( 10, 11 ),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 12 ),
														dataSet.values.projectedValues( 13, 14 )
												),
												null,
												new ObjectDto<>(
														dataSet.values.projectedValue( 15 ),
														dataSet.values.projectedValues( 16, 17 )
												)
										)
								)
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1 ),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 2 ),
														dataSet.values.projectedValues( 3 )
												)
										)
								)
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 3 ),
														dataSet.values.projectedValues( 4, 5 )
												),
												new ObjectDto<>(
														dataSet.values.projectedValue( 6 ),
														dataSet.values.projectedValues( 7, 8 )
												)
										)
								)
						) ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										Collections.emptyList()
								)
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								(Level1ObjectDto<P>) null
						) ),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 0 ),
														dataSet.values.projectedValues( 1, 2 )
												)
										)
								),
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 3 ),
														dataSet.values.projectedValues( 4, 5 )
												)
										)
								)
						) ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										listMaybeWithNull(
												new ObjectDto<>(
														null,
														Collections.emptyList()
												)
										)
								),
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										listMaybeWithNull(
												new ObjectDto<>(
														null,
														Collections.emptyList()
												)
										)
								)
						) ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										Collections.emptyList()
								),
								new Level1ObjectDto<>(
										null,
										Collections.emptyList(),
										Collections.emptyList()
								)
						) )
				);
	}

	@Test
	public void objectOnLevel1AndNoLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( level1Path() )
										.from(
												singleValuedProjection( f, level1SingleValuedFieldPath() ),
												multiValuedProjection( f, level1MultiValuedFieldPath() )
										)
										.as( ObjectDto::new )
										.multi()
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, listMaybeWithNull(
								null,
								new ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 )
								),
								new ObjectDto<>(
										dataSet.values.projectedValue( 9 ),
										dataSet.values.projectedValues( 10, 11 )
								)
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1 )
								)
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 )
								)
						) ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										null,
										Collections.emptyList()
								)
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								(ObjectDto<P>) null
						) ),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										null,
										Collections.emptyList()
								),
								new ObjectDto<>(
										null,
										Collections.emptyList()
								)
						) ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										null,
										Collections.emptyList()
								),
								new ObjectDto<>(
										null,
										Collections.emptyList()
								)
						) ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										null,
										Collections.emptyList()
								),
								new ObjectDto<>(
										null,
										Collections.emptyList()
								)
						) )
				);
	}

	@Test
	public void objectOnLevel1AndFlattenedLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( level1Path() )
										.from(
												singleValuedProjection( f, level1SingleValuedFieldPath() ),
												multiValuedProjection( f, level1MultiValuedFieldPath() ),
												f.composite()
														.from(
																multiValuedProjection( f, level2SingleValuedFieldPath() ),
																multiValuedProjection( f, level2MultiValuedFieldPath() )
														)
														.as( FlattenedObjectDto::new )
										)
										.as( Level1ObjectWithFlattenedLevel2Dto::new )
										.multi()
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, listMaybeWithNull(
								null,
								new Level1ObjectWithFlattenedLevel2Dto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 3, 6 ),
												dataSet.values.projectedValues( 4, 5, 7, 8 )
										)
								),
								new Level1ObjectWithFlattenedLevel2Dto<>(
										dataSet.values.projectedValue( 9 ),
										dataSet.values.projectedValues( 10, 11 ),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 12, 15 ),
												dataSet.values.projectedValues( 13, 14, 16, 17 )
										)
								)
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1 ),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 2 ),
												dataSet.values.projectedValues( 3 )
										)
								)
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 3, 6 ),
												dataSet.values.projectedValues( 4, 5, 7, 8 )
										)
								)
						) ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												Collections.emptyList(),
												Collections.emptyList()
										)
								)
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								(Level1ObjectWithFlattenedLevel2Dto<P>) null
						) ),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 0 ),
												dataSet.values.projectedValues( 1, 2 )
										)
								),
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												dataSet.values.projectedValues( 3 ),
												dataSet.values.projectedValues( 4, 5 )
										)
								)
						) ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												Collections.emptyList(),
												Collections.emptyList()
										)
								),
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												Collections.emptyList(),
												Collections.emptyList()
										)
								)
						) ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												Collections.emptyList(),
												Collections.emptyList()
										)
								),
								new Level1ObjectWithFlattenedLevel2Dto<>(
										null,
										Collections.emptyList(),
										new FlattenedObjectDto<>(
												Collections.emptyList(),
												Collections.emptyList()
										)
								)
						) )
				);
	}

	@Test
	public void objectOnSingleValuedLevel1AndNoLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( singleValuedLevel1Path() )
										.from(
												singleValuedProjection( f, singleValuedLevel1SingleValuedFieldPath() ),
												multiValuedProjection( f, singleValuedLevel1MultiValuedFieldPath() )
										)
										.as( ObjectDto::new )
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, new ObjectDto<>(
								dataSet.values.projectedValue( 18 ),
								dataSet.values.projectedValues( 19, 20 )
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, new ObjectDto<>(
								dataSet.values.projectedValue( 4 ),
								dataSet.values.projectedValues( 5 )
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, new ObjectDto<>(
								dataSet.values.projectedValue( 9 ),
								dataSet.values.projectedValues( 10, 11 )
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, null ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID,
								TckConfiguration.get().getBackendFeatures()
										.projectionPreservesEmptySingleValuedObject( dataSet.singleValuedObjectStructure )
										? new ObjectDto<>(
												null,
												Collections.emptyList() )
										: null
						),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, null ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, null ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, null ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, null )
				);
	}

	@Test
	public void noLevel1AndObjectOnLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( level2Path() )
										.from(
												singleValuedProjection( f, level2SingleValuedFieldPath() ),
												multiValuedProjection( f, level2MultiValuedFieldPath() )
										)
										.as( ObjectDto::new )
										.multi()
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, listMaybeWithNull(
								null,
								new ObjectDto<>(
										dataSet.values.projectedValue( 3 ),
										dataSet.values.projectedValues( 4, 5 )
								),
								new ObjectDto<>(
										dataSet.values.projectedValue( 6 ),
										dataSet.values.projectedValues( 7, 8 )
								),
								new ObjectDto<>(
										dataSet.values.projectedValue( 12 ),
										dataSet.values.projectedValues( 13, 14 )
								),
								null,
								new ObjectDto<>(
										dataSet.values.projectedValue( 15 ),
										dataSet.values.projectedValues( 16, 17 )
								)
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										dataSet.values.projectedValue( 2 ),
										dataSet.values.projectedValues( 3 )
								)
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										dataSet.values.projectedValue( 3 ),
										dataSet.values.projectedValues( 4, 5 )
								),
								new ObjectDto<>(
										dataSet.values.projectedValue( 6 ),
										dataSet.values.projectedValues( 7, 8 )
								)
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, Collections.emptyList() ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 )
								),
								new ObjectDto<>(
										dataSet.values.projectedValue( 3 ),
										dataSet.values.projectedValues( 4, 5 )
								)
						) ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, listMaybeWithNull(
								new ObjectDto<>(
										null,
										Collections.emptyList()
								),
								new ObjectDto<>(
										null,
										Collections.emptyList()
								)
						) ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, Collections.emptyList() )
				);
	}

	@Test
	public void flattenedLevel1AndObjectOnLevel2() {
		assertThatQuery( mainIndex.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.composite()
										.from(
												multiValuedProjection( f, level1SingleValuedFieldPath() ),
												multiValuedProjection( f, level1MultiValuedFieldPath() ),
												f.object( level2Path() )
														.from(
																singleValuedProjection( f, level2SingleValuedFieldPath() ),
																multiValuedProjection( f, level2MultiValuedFieldPath() )
														)
														.as( ObjectDto::new )
														.multi()
										)
										.as( FlattenedLevel1ObjectDto::new )
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.containsExactlyInAnyOrder(
						hit( FULL_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								dataSet.values.projectedValues( 0, 9 ),
								dataSet.values.projectedValues( 1, 2, 10, 11 ),
								listMaybeWithNull(
										null,
										new ObjectDto<>(
												dataSet.values.projectedValue( 3 ),
												dataSet.values.projectedValues( 4, 5 )
										),
										new ObjectDto<>(
												dataSet.values.projectedValue( 6 ),
												dataSet.values.projectedValues( 7, 8 )
										),
										new ObjectDto<>(
												dataSet.values.projectedValue( 12 ),
												dataSet.values.projectedValues( 13, 14 )
										),
										null,
										new ObjectDto<>(
												dataSet.values.projectedValue( 15 ),
												dataSet.values.projectedValues( 16, 17 )
										)
								)
						) ),
						hit( SINGLE_VALUED_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								dataSet.values.projectedValues( 0 ),
								dataSet.values.projectedValues( 1 ),
								listMaybeWithNull(
										new ObjectDto<>(
												dataSet.values.projectedValue( 2 ),
												dataSet.values.projectedValues( 3 )
										)
								)
						) ),
						hit( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								dataSet.values.projectedValues( 0 ),
								dataSet.values.projectedValues( 1, 2 ),
								listMaybeWithNull(
										new ObjectDto<>(
												dataSet.values.projectedValue( 3 ),
												dataSet.values.projectedValues( 4, 5 )
										),
										new ObjectDto<>(
												dataSet.values.projectedValue( 6 ),
												dataSet.values.projectedValues( 7, 8 )
										)
								)
						) ),
						hit( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								Collections.emptyList()
						) ),
						hit( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								Collections.emptyList()
						) ),
						hit( LEVEL1_NO_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								Collections.emptyList()
						) ),
						hit( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								listMaybeWithNull(
										new ObjectDto<>(
												dataSet.values.projectedValue( 0 ),
												dataSet.values.projectedValues( 1, 2 )
										),
										new ObjectDto<>(
												dataSet.values.projectedValue( 3 ),
												dataSet.values.projectedValues( 4, 5 )
										)
								)
						) ),
						hit( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								listMaybeWithNull(
										new ObjectDto<>(
												null,
												Collections.emptyList()
										),
										new ObjectDto<>(
												null,
												Collections.emptyList()
										)
								)
						) ),
						hit( LEVEL2_NO_OBJECT_DOCUMENT_ID, new FlattenedLevel1ObjectDto<>(
								Collections.emptyList(),
								Collections.emptyList(),
								Collections.emptyList()
						) )
				);
	}

	@Test
	public void fieldOutsideObjectFieldTree() {
		assertThatThrownBy( () -> mainIndex.query()
						.select( f -> f.object( level2Path() )
								.from(
										// This is incorrect: the inner projection uses fields from "level1",
										// which won't be present in "level1.level2".
										singleValuedProjection( f, level1SingleValuedFieldPath() )
								)
								.asList()
								.multi() )
						.where( f -> f.matchAll() )
						.routing( dataSet.routingKey )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid context for projection on field '" + level1SingleValuedFieldPath() + "'",
						"the surrounding projection is executed for each object in field '" + level2Path() + "',"
								+ " which is not a parent of field '" + level1SingleValuedFieldPath() + "'",
						"Check the structure of your projections"
				);
	}

	@Test
	public void singleValuedField_effectivelyMultiValuedInContext() {
		assertThatThrownBy( () -> mainIndex.query()
						.select( f -> f.object( level1Path() )
								.from(
										singleValuedProjection( f, level1SingleValuedFieldPath() ),
										multiValuedProjection( f, level1MultiValuedFieldPath() ),
										f.composite()
												.from(
														// This is incorrect: we don't use object( "level1.level2" ),
														// so this field is multi-valued, because it's collected
														// for each "level1" object, and "level1.level2" is multi-valued.
														singleValuedProjection( f, level2SingleValuedFieldPath() ),
														multiValuedProjection( f, level2MultiValuedFieldPath() )
												)
												.asList()
								)
								.asList()
								.multi() )
						.where( f -> f.matchAll() )
						.routing( dataSet.routingKey )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid cardinality for projection on field '" + level2SingleValuedFieldPath() + "'",
						"the projection is single-valued, but this field is effectively multi-valued in this context",
						"because parent object field '" + level2Path() + "' is multi-valued",
						"call '.multi()' when you create the projection on field '" + level2SingleValuedFieldPath() + "'",
						"or wrap that projection in an object projection like this:"
								+ " 'f.object(\"" + level2Path() + "\").from(<the projection on field " + level2SingleValuedFieldPath() + ">).as(...).multi()'."
				);
	}

	@Test
	public void missingFields() {
		StubMappingScope scope = mainIndex.createScope( missingLevel1Index, missingLevel1SingleValuedFieldIndex,
				missingLevel2Index, missingLevel2SingleValuedFieldIndex
		);
		assertThatQuery( scope.query()
				.select( f -> f.composite()
						.from(
								f.id( String.class ),
								f.object( level1Path() )
										.from(
												singleValuedProjection( f, level1SingleValuedFieldPath() ),
												multiValuedProjection( f, level1MultiValuedFieldPath() ),
												f.object( level2Path() )
														.from(
																singleValuedProjection( f, level2SingleValuedFieldPath() ),
																multiValuedProjection( f, level2MultiValuedFieldPath() )
														)
														.as( ObjectDto::new )
														.multi()
										)
										.as( Level1ObjectDto::new )
										.multi()
						)
						.as( IdAndObjectDto::new ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey ) )
				.hits().asIs().usingRecursiveFieldByFieldElementComparator( recursiveComparisonConfig )
				.contains(
						hit( MISSING_LEVEL1_DOCUMENT_ID, Collections.emptyList() ),
						hit( MISSING_LEVEL1_SINGLE_VALUED_FIELD_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										null,
										dataSet.values.projectedValues( 0, 1 ),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 2 ),
														dataSet.values.projectedValues( 3, 4 )
												),
												new ObjectDto<>(
														dataSet.values.projectedValue( 5 ),
														dataSet.values.projectedValues( 6, 7 )
												)
										)
								),
								new Level1ObjectDto<>(
										null,
										dataSet.values.projectedValues( 8, 9 ),
										listMaybeWithNull(
												new ObjectDto<>(
														dataSet.values.projectedValue( 10 ),
														dataSet.values.projectedValues( 11, 12 )
												),
												new ObjectDto<>(
														dataSet.values.projectedValue( 13 ),
														dataSet.values.projectedValues( 14, 15 )
												)
										)
								)
						) ),
						hit( MISSING_LEVEL2_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										Collections.emptyList()
								),
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 3 ),
										dataSet.values.projectedValues( 4, 5 ),
										Collections.emptyList()
								)
						) ),
						hit( MISSING_LEVEL2_SINGLE_VALUED_FIELD_DOCUMENT_ID, listMaybeWithNull(
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 0 ),
										dataSet.values.projectedValues( 1, 2 ),
										listMaybeWithNull(
												new ObjectDto<>(
														null,
														dataSet.values.projectedValues( 3, 4 )
												),
												new ObjectDto<>(
														null,
														dataSet.values.projectedValues( 5, 6 )
												)
										)
								),
								new Level1ObjectDto<>(
										dataSet.values.projectedValue( 7 ),
										dataSet.values.projectedValues( 8, 9 ),
										listMaybeWithNull(
												new ObjectDto<>(
														null,
														dataSet.values.projectedValues( 10, 11 )
												),
												new ObjectDto<>(
														null,
														dataSet.values.projectedValues( 12, 13 )
												)
										)
								)
						) )
				);
	}

	@SafeVarargs
	private static <T> List<T> listMaybeWithNull(T ... values) {
		List<T> list = new ArrayList<>();
		Collections.addAll( list, values );
		if ( !TckConfiguration.get().getBackendFeatures().projectionPreservesNulls() ) {
			list.removeIf( Objects::isNull );
		}
		return list;
	}

	private <T> IdAndObjectDto<T> hit(String docIdConstant, T object) {
		return new IdAndObjectDto<>( dataSet.docId( docIdConstant ), object );
	}

	private String level1Path() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure ).absolutePath;
	}

	private String level1SingleValuedFieldPath() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure )
				.singleValuedFieldAbsolutePath( dataSet.fieldType );
	}

	private String level1MultiValuedFieldPath() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure )
				.multiValuedFieldAbsolutePath( dataSet.fieldType );
	}

	private String singleValuedLevel1Path() {
		return mainIndex.binding().singleValuedLevel1( dataSet.singleValuedObjectStructure ).absolutePath;
	}

	private String singleValuedLevel1SingleValuedFieldPath() {
		return mainIndex.binding().singleValuedLevel1( dataSet.singleValuedObjectStructure )
				.singleValuedFieldAbsolutePath( dataSet.fieldType );
	}

	private String singleValuedLevel1MultiValuedFieldPath() {
		return mainIndex.binding().singleValuedLevel1( dataSet.singleValuedObjectStructure )
				.multiValuedFieldAbsolutePath( dataSet.fieldType );
	}

	private String level2Path() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure ).level2.absolutePath;
	}

	private String level2SingleValuedFieldPath() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure ).level2
				.singleValuedFieldAbsolutePath( dataSet.fieldType );
	}

	private String level2MultiValuedFieldPath() {
		return mainIndex.binding().level1( dataSet.multiValuedObjectStructure ).level2
				.multiValuedFieldAbsolutePath( dataSet.fieldType );
	}

	protected abstract ProjectionFinalStep<P> singleValuedProjection(SearchProjectionFactory<?, ?> f, String absoluteFieldPath);

	protected abstract ProjectionFinalStep<List<P>> multiValuedProjection(SearchProjectionFactory<?, ?> f, String absoluteFieldPath);

	public static final class DataSet<F, P, V extends AbstractProjectionTestValues<F, P>>
			extends AbstractPerFieldTypeProjectionDataSet<F, P, V> {
		private final ObjectStructure singleValuedObjectStructure;
		private final ObjectStructure multiValuedObjectStructure;

		public DataSet(V values, ObjectStructure singleValuedObjectStructure,
				ObjectStructure multiValuedObjectStructure) {
			super( values.fieldType.getUniqueName() + "/single=" + singleValuedObjectStructure.name()
					+ "/multi=" + multiValuedObjectStructure, values );
			this.singleValuedObjectStructure = singleValuedObjectStructure;
			this.multiValuedObjectStructure = multiValuedObjectStructure;
		}

		public String docId(String docIdConstant) {
			return docIdConstant + "_" + routingKey;
		}

		public void contribute(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
				SimpleMappedIndex<MissingLevel1IndexBinding> missingLevel1Index, BulkIndexer missingLevel1Indexer,
				SimpleMappedIndex<MissingLevel1SingleValuedFieldIndexBinding> missingLevel1SingleValuedFieldIndex,
				BulkIndexer missingLevel1SingleValuedFieldIndexer,
				SimpleMappedIndex<MissingLevel2IndexBinding> missingLevel2Index, BulkIndexer missingLevel2Indexer,
				SimpleMappedIndex<MissingLevel2SingleValuedFieldIndexBinding> missingLevel2SingleValuedFieldIndex,
				BulkIndexer missingLevel2SingleValuedFieldIndexer) {
			mainIndexer
					.add( docId( FULL_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						ObjectFieldBinding level2;
						Level1ObjectFieldBinding singleValuedLevel1;
						DocumentElement level1Object;
						DocumentElement level2Object;
						DocumentElement singleValuedLevel1Object;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						level2 = level1.level2;
						singleValuedLevel1 = mainIndex.binding().singleValuedLevel1( singleValuedObjectStructure );

						document.addNullObject( level1.reference );

						level1Object = document.addObject( level1.reference );
						level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );
						level1Object.addNullObject( level2.reference );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 6 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 7 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 8 ) );

						level1Object = document.addObject( level1.reference );
						level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 9 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 10 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 11 ) );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 12 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 13 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 14 ) );
						level1Object.addNullObject( level2.reference );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 15 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 16 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 17 ) );

						singleValuedLevel1Object = document.addObject( singleValuedLevel1.reference );
						singleValuedLevel1Object.addValue( singleValuedLevel1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 18 ) );
						singleValuedLevel1Object.addValue( singleValuedLevel1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 19 ) );
						singleValuedLevel1Object.addValue( singleValuedLevel1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 20 ) );
					} )
					.add( docId( SINGLE_VALUED_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						ObjectFieldBinding level2;
						Level1ObjectFieldBinding singleValuedLevel1;
						DocumentElement level1Object;
						DocumentElement level2Object;
						DocumentElement singleValuedLevel1Object;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						level2 = level1.level2;
						singleValuedLevel1 = mainIndex.binding().singleValuedLevel1( singleValuedObjectStructure );

						level1Object = document.addObject( level1.reference );
						level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );

						singleValuedLevel1Object = document.addObject( singleValuedLevel1.reference );
						singleValuedLevel1Object.addValue( singleValuedLevel1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
						singleValuedLevel1Object.addValue( singleValuedLevel1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
					} )
					.add( docId( LEVEL1_SINGLE_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						ObjectFieldBinding level2;
						Level1ObjectFieldBinding singleValuedLevel1;
						DocumentElement level1Object;
						DocumentElement level2Object;
						DocumentElement singleValuedLevel1Object;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						level2 = level1.level2;
						singleValuedLevel1 = mainIndex.binding().singleValuedLevel1( singleValuedObjectStructure );

						level1Object = document.addObject( level1.reference );
						level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
						level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 6 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 7 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 8 ) );

						singleValuedLevel1Object = document.addObject( singleValuedLevel1.reference );
						singleValuedLevel1Object.addValue( singleValuedLevel1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 9 ) );
						singleValuedLevel1Object.addValue( singleValuedLevel1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 10 ) );
						singleValuedLevel1Object.addValue( singleValuedLevel1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 11 ) );
					} )
					.add( docId( LEVEL1_SINGLE_EMPTY_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						Level1ObjectFieldBinding singleValuedLevel1;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						singleValuedLevel1 = mainIndex.binding().singleValuedLevel1( singleValuedObjectStructure );

						document.addObject( level1.reference );
						document.addObject( singleValuedLevel1.reference );
					} )
					.add( docId( LEVEL1_SINGLE_NULL_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						Level1ObjectFieldBinding singleValuedLevel1;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						singleValuedLevel1 = mainIndex.binding().singleValuedLevel1( singleValuedObjectStructure );

						document.addNullObject( level1.reference );
						document.addNullObject( singleValuedLevel1.reference );
					} )
					.add( docId( LEVEL1_NO_OBJECT_DOCUMENT_ID ), routingKey, document -> {
					} )
					.add( docId( LEVEL2_SINGLE_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						ObjectFieldBinding level2;
						DocumentElement level1Object;
						DocumentElement level2Object;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						level2 = level1.level2;

						level1Object = document.addObject( level1.reference );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );

						level1Object = document.addObject( level1.reference );
						level2Object = level1Object.addObject( level2.reference );
						level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
						level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
					} )
					.add( docId( LEVEL2_SINGLE_EMPTY_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;
						ObjectFieldBinding level2;
						DocumentElement level1Object;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );
						level2 = level1.level2;

						level1Object = document.addObject( level1.reference );
						level1Object.addObject( level2.reference );

						level1Object = document.addObject( level1.reference );
						level1Object.addObject( level2.reference );
					} )
					.add( docId( LEVEL2_NO_OBJECT_DOCUMENT_ID ), routingKey, document -> {
						Level1ObjectFieldBinding level1;

						level1 = mainIndex.binding().level1( multiValuedObjectStructure );

						document.addObject( level1.reference );

						document.addObject( level1.reference );
					} );
			missingLevel1Indexer.add( docId( MISSING_LEVEL1_DOCUMENT_ID ), routingKey, document -> { } );
			missingLevel1SingleValuedFieldIndexer.add( docId( MISSING_LEVEL1_SINGLE_VALUED_FIELD_DOCUMENT_ID ), routingKey, document -> {
				Level1ObjectFieldBindingWithoutSingleValuedField level1;
				ObjectFieldBinding level2;
				DocumentElement level1Object;
				DocumentElement level2Object;

				level1 = missingLevel1SingleValuedFieldIndex.binding().level1( multiValuedObjectStructure );
				level2 = level1.level2;

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 6 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 7 ) );

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 8 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 9 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 10 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 11 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 12 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 13 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 14 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 15 ) );
			} );
			missingLevel2Indexer.add( docId( MISSING_LEVEL2_DOCUMENT_ID ), routingKey, document -> {
				ObjectFieldBinding level1;
				DocumentElement level1Object;

				level1 = missingLevel2Index.binding().level1( multiValuedObjectStructure );

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
			} );
			missingLevel2SingleValuedFieldIndexer.add( docId( MISSING_LEVEL2_SINGLE_VALUED_FIELD_DOCUMENT_ID ), routingKey, document -> {
				Level1ObjectFieldBindingWithoutLevel2SingleValuedField level1;
				ObjectFieldBindingWithoutSingleValuedField level2;
				DocumentElement level1Object;
				DocumentElement level2Object;

				level1 = missingLevel2SingleValuedFieldIndex.binding().level1( multiValuedObjectStructure );
				level2 = level1.level2;

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 0 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 1 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 2 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 3 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 4 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 5 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 6 ) );

				level1Object = document.addObject( level1.reference );
				level1Object.addValue( level1.singleValuedField.get( values.fieldType ).reference, values.fieldValue( 7 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 8 ) );
				level1Object.addValue( level1.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 9 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 10 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 11 ) );
				level2Object = level1Object.addObject( level2.reference );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 12 ) );
				level2Object.addValue( level2.multiValuedField.get( values.fieldType ).reference, values.fieldValue( 13 ) );
			} );
		}
	}

	static class IdAndObjectDto<T> {
		final String id;
		final T object;

		IdAndObjectDto(String id, T object) {
			this.id = id;
			this.object = object;
		}

		@Override
		public String toString() {
			return "IdAndObjectDto{" +
					"id='" + id + '\'' +
					", object=" + object +
					'}';
		}
	}

	static class ObjectDto<T> {
		final T singleValuedField;
		final List<T> multiValuedField;

		ObjectDto(T singleValuedField, List<T> multiValuedField) {
			this.singleValuedField = singleValuedField;
			this.multiValuedField = multiValuedField;
		}

		@Override
		public String toString() {
			return "ObjectDto{" +
					"singleValuedField=" + singleValuedField +
					", multiValuedField=" + multiValuedField +
					'}';
		}
	}

	static class Level1ObjectDto<T> extends ObjectDto<T> {
		final List<ObjectDto<T>> level2;

		Level1ObjectDto(T singleValuedField, List<T> multiValuedField, List<ObjectDto<T>> level2) {
			super( singleValuedField, multiValuedField );
			this.level2 = level2;
		}

		@Override
		public String toString() {
			return "Level1ObjectDto{" +
					"singleValuedField=" + singleValuedField +
					", multiValuedField=" + multiValuedField +
					", level2=" + level2 +
					"}";
		}
	}

	static class FlattenedObjectDto<T> {
		final List<T> singleValuedField;
		final List<T> multiValuedField;

		FlattenedObjectDto(List<T> singleValuedField, List<T> multiValuedField) {
			this.singleValuedField = singleValuedField;
			this.multiValuedField = multiValuedField;
		}

		@Override
		public String toString() {
			return "FlattenedObjectDto{" +
					"singleValuedField=" + singleValuedField +
					", multiValuedField=" + multiValuedField +
					'}';
		}
	}

	static class Level1ObjectWithFlattenedLevel2Dto<T> extends ObjectDto<T> {
		final FlattenedObjectDto<T> level2;

		Level1ObjectWithFlattenedLevel2Dto(T singleValuedField, List<T> multiValuedField, FlattenedObjectDto<T> level2) {
			super( singleValuedField, multiValuedField );
			this.level2 = level2;
		}

		@Override
		public String toString() {
			return "Level1ObjectWithFlattenedLevel2Dto{" +
					"singleValuedField=" + singleValuedField +
					", multiValuedField=" + multiValuedField +
					", level2=" + level2 +
					"}";
		}
	}

	static class FlattenedLevel1ObjectDto<T> extends FlattenedObjectDto<T> {
		final List<ObjectDto<T>> level2;

		FlattenedLevel1ObjectDto(List<T> singleValuedField, List<T> multiValuedField, List<ObjectDto<T>> level2) {
			super( singleValuedField, multiValuedField );
			this.level2 = level2;
		}

		@Override
		public String toString() {
			return "FlattenedLevel1ObjectDto{" +
					"singleValuedField=" + singleValuedField +
					", multiValuedField=" + multiValuedField +
					", level2=" + level2 +
					'}';
		}
	}

	protected static class IndexBinding {
		private final Level1ObjectFieldBinding level1_nested;
		private final Level1ObjectFieldBinding level1_flattened;
		private final Level1ObjectFieldBinding singleValuedLevel1_nested;
		private final Level1ObjectFieldBinding singleValuedLevel1_flattened;

		IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			IndexSchemaObjectField level1NestedObjectField =
					root.objectField( "level1_nested", ObjectStructure.NESTED ).multiValued();
			level1_nested = new Level1ObjectFieldBinding( level1NestedObjectField, null,
					"level1_nested", ObjectStructure.NESTED, fieldTypes );
			IndexSchemaObjectField level1FlattenedObjectField =
					root.objectField( "level1_flattened", ObjectStructure.FLATTENED ).multiValued();
			level1_flattened = new Level1ObjectFieldBinding( level1FlattenedObjectField, null,
					"level1_flattened", ObjectStructure.FLATTENED, fieldTypes );
			IndexSchemaObjectField singleValuedLevel1NestedObjectField =
					root.objectField( "singleValuedLevel1_nested", ObjectStructure.NESTED );
			singleValuedLevel1_nested = new Level1ObjectFieldBinding( singleValuedLevel1NestedObjectField, null,
					"singleValuedLevel1_nested", ObjectStructure.NESTED, fieldTypes );
			IndexSchemaObjectField singleValuedLevel1FlattenedObjectField =
					root.objectField( "singleValuedLevel1_flattened", ObjectStructure.FLATTENED );
			singleValuedLevel1_flattened = new Level1ObjectFieldBinding( singleValuedLevel1FlattenedObjectField, null,
					"singleValuedLevel1_flattened", ObjectStructure.FLATTENED, fieldTypes );
		}

		public Level1ObjectFieldBinding level1(ObjectStructure structure) {
			return ObjectStructure.NESTED.equals( structure ) ? level1_nested : level1_flattened;
		}

		public Level1ObjectFieldBinding singleValuedLevel1(ObjectStructure structure) {
			return ObjectStructure.NESTED.equals( structure ) ? singleValuedLevel1_nested : singleValuedLevel1_flattened;
		}
	}

	protected static class MissingLevel1IndexBinding {
		MissingLevel1IndexBinding(IndexSchemaElement root) {
		}
	}

	protected static class MissingLevel1SingleValuedFieldIndexBinding {
		final Level1ObjectFieldBindingWithoutSingleValuedField level1_nested;
		final Level1ObjectFieldBindingWithoutSingleValuedField level1_flattened;

		MissingLevel1SingleValuedFieldIndexBinding(IndexSchemaElement root,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			IndexSchemaObjectField level1NestedObjectField =
					root.objectField( "level1_nested", ObjectStructure.NESTED ).multiValued();
			level1_nested = new Level1ObjectFieldBindingWithoutSingleValuedField( level1NestedObjectField, null,
					"level1_nested", ObjectStructure.NESTED, fieldTypes );
			IndexSchemaObjectField level1FlattenedObjectField =
					root.objectField( "level1_flattened", ObjectStructure.FLATTENED ).multiValued();
			level1_flattened = new Level1ObjectFieldBindingWithoutSingleValuedField( level1FlattenedObjectField, null,
					"level1_flattened", ObjectStructure.FLATTENED, fieldTypes );
		}

		public Level1ObjectFieldBindingWithoutSingleValuedField level1(ObjectStructure structure) {
			return ObjectStructure.NESTED.equals( structure ) ? level1_nested : level1_flattened;
		}
	}

	protected static class MissingLevel2IndexBinding {
		final ObjectFieldBinding level1_nested;
		final ObjectFieldBinding level1_flattened;

		MissingLevel2IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			IndexSchemaObjectField level1NestedObjectField =
					root.objectField( "level1_nested", ObjectStructure.NESTED ).multiValued();
			level1_nested = new ObjectFieldBinding( level1NestedObjectField, null,
					"level1_nested", fieldTypes );
			IndexSchemaObjectField level1FlattenedObjectField =
					root.objectField( "level1_flattened", ObjectStructure.FLATTENED ).multiValued();
			level1_flattened = new ObjectFieldBinding( level1FlattenedObjectField, null,
					"level1_flattened", fieldTypes );
		}

		public ObjectFieldBinding level1(ObjectStructure structure) {
			return ObjectStructure.NESTED.equals( structure ) ? level1_nested : level1_flattened;
		}
	}

	protected static class MissingLevel2SingleValuedFieldIndexBinding {
		final Level1ObjectFieldBindingWithoutLevel2SingleValuedField level1_nested;
		final Level1ObjectFieldBindingWithoutLevel2SingleValuedField level1_flattened;

		MissingLevel2SingleValuedFieldIndexBinding(IndexSchemaElement root,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			IndexSchemaObjectField level1NestedObjectField =
					root.objectField( "level1_nested", ObjectStructure.NESTED ).multiValued();
			level1_nested = new Level1ObjectFieldBindingWithoutLevel2SingleValuedField( level1NestedObjectField, null,
					"level1_nested", ObjectStructure.NESTED, fieldTypes );
			IndexSchemaObjectField level1FlattenedObjectField =
					root.objectField( "level1_flattened", ObjectStructure.FLATTENED ).multiValued();
			level1_flattened = new Level1ObjectFieldBindingWithoutLevel2SingleValuedField( level1FlattenedObjectField, null,
					"level1_flattened", ObjectStructure.FLATTENED, fieldTypes );
		}

		public Level1ObjectFieldBindingWithoutLevel2SingleValuedField level1(ObjectStructure structure) {
			return ObjectStructure.NESTED.equals( structure ) ? level1_nested : level1_flattened;
		}
	}

	protected static class ObjectFieldBinding {
		final String absolutePath;
		final SimpleFieldModelsByType singleValuedField;
		final SimpleFieldModelsByType multiValuedField;
		final IndexObjectFieldReference reference;

		ObjectFieldBinding(IndexSchemaObjectField objectField, String parentAbsolutePath, String relativeFieldName,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			absolutePath = FieldPaths.compose( parentAbsolutePath, relativeFieldName );
			singleValuedField = SimpleFieldModelsByType.mapAll( fieldTypes, objectField, "singleValuedField_",
					b -> b.projectable( Projectable.YES ) );
			multiValuedField = SimpleFieldModelsByType.mapAllMultiValued( fieldTypes, objectField, "multiValuedField_",
					b -> b.projectable( Projectable.YES ) );
			reference = objectField.toReference();
		}

		String singleValuedFieldAbsolutePath(FieldTypeDescriptor<?> fieldType) {
			return FieldPaths.compose( absolutePath, singleValuedField.get( fieldType ).relativeFieldName );
		}

		String multiValuedFieldAbsolutePath(FieldTypeDescriptor<?> fieldType) {
			return FieldPaths.compose( absolutePath, multiValuedField.get( fieldType ).relativeFieldName );
		}
	}

	protected static class Level1ObjectFieldBinding extends ObjectFieldBinding {
		final ObjectFieldBinding level2;

		Level1ObjectFieldBinding(IndexSchemaObjectField objectField, String parentAbsolutePath, String relativeFieldName,
				ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			super( objectField, parentAbsolutePath, relativeFieldName, fieldTypes );
			IndexSchemaObjectField level2ObjectField = objectField.objectField( "level2", structure )
					.multiValued();
			level2 = new ObjectFieldBinding( level2ObjectField, this.absolutePath, "level2", fieldTypes );
		}
	}

	protected static class ObjectFieldBindingWithoutSingleValuedField {
		final String absolutePath;
		final SimpleFieldModelsByType multiValuedField;
		final IndexObjectFieldReference reference;

		ObjectFieldBindingWithoutSingleValuedField(IndexSchemaObjectField objectField, String parentAbsolutePath,
				String relativeFieldName, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			absolutePath = FieldPaths.compose( parentAbsolutePath, relativeFieldName );
			multiValuedField = SimpleFieldModelsByType.mapAllMultiValued( fieldTypes, objectField, "multiValuedField_",
					b -> b.projectable( Projectable.YES ) );
			reference = objectField.toReference();
		}
	}

	protected static class Level1ObjectFieldBindingWithoutSingleValuedField
			extends ObjectFieldBindingWithoutSingleValuedField {
		final ObjectFieldBinding level2;

		Level1ObjectFieldBindingWithoutSingleValuedField(IndexSchemaObjectField objectField, String parentAbsolutePath,
				String relativeFieldName,
				ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			super( objectField, parentAbsolutePath, relativeFieldName, fieldTypes );
			IndexSchemaObjectField level2ObjectField = objectField.objectField( "level2", structure )
					.multiValued();
			level2 = new ObjectFieldBinding( level2ObjectField, this.absolutePath, "level2", fieldTypes );
		}
	}

	protected static class Level1ObjectFieldBindingWithoutLevel2SingleValuedField extends ObjectFieldBinding {
		final ObjectFieldBindingWithoutSingleValuedField level2;

		Level1ObjectFieldBindingWithoutLevel2SingleValuedField(IndexSchemaObjectField objectField, String parentAbsolutePath,
				String relativeFieldName,
				ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			super( objectField, parentAbsolutePath, relativeFieldName, fieldTypes );
			IndexSchemaObjectField level2ObjectField = objectField.objectField( "level2", structure )
					.multiValued();
			level2 = new ObjectFieldBindingWithoutSingleValuedField( level2ObjectField, this.absolutePath, "level2", fieldTypes );
		}
	}
}
