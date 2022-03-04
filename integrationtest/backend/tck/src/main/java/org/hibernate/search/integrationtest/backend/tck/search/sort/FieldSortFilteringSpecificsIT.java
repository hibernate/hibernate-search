/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior related to
 * {@link org.hibernate.search.engine.search.sort.dsl.SortFilterStep#filter(Function) filtering}
 * that is not tested in {@link FieldSortBaseIT}.
 */
@RunWith(Parameterized.class)
public class FieldSortFilteringSpecificsIT<F> {

	private static Stream<FieldTypeDescriptor<?>> supportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.isFieldSortSupported() );
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		supportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			parameters.add( new Object[] { fieldTypeDescriptor } );
		} );
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSortFilteringSpecificsIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	public void nonNested() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName + "."
				+ index.binding().flattenedObject.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.field( fieldPath )
						.filter( pf -> pf.exists().field( fieldPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort filter: field '" + fieldPath + "' is not contained in a nested object.",
						"Sort filters are only available if the field to sort on is contained in a nested object."
				);
	}

	@Test
	public void invalidNestedPath_parent() {
		String fieldPath = index.binding().nestedObject1.relativeFieldName + "."
				+ index.binding().nestedObject1.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
		String fieldInParentPath = index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.field( fieldPath ).filter( pf -> pf.exists().field( fieldInParentPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInParentPath + "]",
						"only fields that are contained in the nested object with path '"
								+ index.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	@Test
	public void invalidNestedPath_sibling() {
		String fieldPath = index.binding().nestedObject1.relativeFieldName + "."
				+ index.binding().nestedObject1.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
		String fieldInSiblingPath = index.binding().nestedObject2.relativeFieldName + "."
				+ index.binding().nestedObject2.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThatThrownBy(
				() -> matchAllQuery( f -> f.field( fieldPath ).filter( pf -> pf.exists().field( fieldInSiblingPath ) ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInSiblingPath + "]",
						"only fields that are contained in the nested object with path '"
								+ index.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchAllQuery( sortContributor, index.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private static class AbstractObjectMapping {
		final SimpleFieldModelsByType fieldModels;

		AbstractObjectMapping(IndexSchemaElement self) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedTypeDescriptors(), self,
					"", c -> c.sortable( Sortable.YES ) );
		}
	}

	private static class IndexBinding extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject1;
		final FirstLevelObjectMapping nestedObject2;

		IndexBinding(IndexSchemaElement root) {
			super( root );

			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectStructure.FLATTENED );
			nestedObject1 = FirstLevelObjectMapping.create( root, "nestedObject1",
					ObjectStructure.NESTED );
			nestedObject2 = FirstLevelObjectMapping.create( root, "nestedObject2",
					ObjectStructure.NESTED );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure) {
			return create( parent, relativeFieldName, structure, false );
		}

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new FirstLevelObjectMapping( relativeFieldName, objectField );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}
}
