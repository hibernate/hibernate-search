/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class VectorFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withSingleBackend(
			MethodHandles.lookup(), BackendConfigurations.simple() );

	/*
	* This test relies on a backend implementation to make sure that the vector dimension was somehow set for the field.
	* hence it requires a real backend.
	 */
	@Test
	void customBridge_vectorDimensionUnknown() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBinder = @ValueBinderRef(type = ValidImplicitTypeBridge.ValidImplicitTypeBinder.class))
			Collection<Float> floats;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".floats" )
						.indexContext( INDEX_NAME )
						.failure(
								"Invalid index field type: missing vector dimension."
										+ " Define the vector dimension explicitly."
										// hint:
										+ " Either specify dimension as an annotation property (@VectorField(dimension = somePositiveInteger)), or define a value binder (@VectorField(valueBinder = @ValueBinderRef(..))) that explicitly declares a vector field specifying the dimension."
						) );
	}

	@SuppressWarnings("rawtypes")
	public static class ValidImplicitTypeBridge implements ValueBridge<Collection, float[]> {

		public static class ValidImplicitTypeBinder implements ValueBinder {

			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( Collection.class, new ValidImplicitTypeBridge() );
			}
		}

		@Override
		public float[] toIndexedValue(Collection value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			float[] result = new float[value.size()];
			int index = 0;
			for ( Object o : value ) {
				result[index++] = Float.parseFloat( Objects.toString( o, null ) );
			}
			return result;
		}

		@Override
		public Collection fromIndexedValue(float[] value, ValueBridgeFromIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			List<Float> floats = new ArrayList<>( value.length );
			for ( float v : value ) {
				floats.add( v );
			}
			return floats;
		}
	}
}
