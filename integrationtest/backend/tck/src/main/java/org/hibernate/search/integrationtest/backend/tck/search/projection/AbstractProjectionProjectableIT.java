/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractProjectionProjectableIT {

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void projectable_default_use(
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableDefaultIndexBinding> projectableDefaultIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableYesIndexBinding> projectableYesIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableNoIndexBinding> projectableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		assumeFalse(
				TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault(),
				"Skipping this test as the backend makes fields projectable by default."
		);

		SearchProjectionFactory<?, ?> f = projectableDefaultIndex.createScope().projection();

		String fieldPath = projectableDefaultIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryProjection( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + projectionTrait() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void projectable_no_use(
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableDefaultIndexBinding> projectableDefaultIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableYesIndexBinding> projectableYesIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableNoIndexBinding> projectableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchProjectionFactory<?, ?> f = projectableNoIndex.createScope().projection();

		String fieldPath = projectableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryProjection( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + projectionTrait() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}


	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void multiIndex_incompatibleProjectable(
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableDefaultIndexBinding> projectableDefaultIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableYesIndexBinding> projectableYesIndex,
			SimpleMappedIndex<AbstractProjectionProjectableIT.ProjectableNoIndexBinding> projectableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchProjectionFactory<?, ?> f = projectableYesIndex.createScope( projectableNoIndex ).projection();

		String fieldPath = projectableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryProjection( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + projectionTrait() + "'"
				);
	}

	protected abstract void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath,
			FieldTypeDescriptor<?, ?> fieldType);

	protected abstract String projectionTrait();

	public static final class ProjectableDefaultIndexBinding {
		private final SimpleFieldModelsByType field;

		public ProjectableDefaultIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
		}
	}

	public static final class ProjectableYesIndexBinding {
		private final SimpleFieldModelsByType field;

		public ProjectableYesIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.projectable( Projectable.YES ) );
		}
	}

	public static final class ProjectableNoIndexBinding {
		private final SimpleFieldModelsByType field;

		public ProjectableNoIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.projectable( Projectable.NO ) );
		}
	}

}
