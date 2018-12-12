/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultValueBridgeExpectations;

public abstract class PropertyTypeDescriptor<V> {

	private static List<PropertyTypeDescriptor<?>> all;

	public static List<PropertyTypeDescriptor<?>> getAll() {
		if ( all == null ) {
			all = Collections.unmodifiableList( Arrays.asList(
					new BoxedIntegerPropertyTypeDescriptor(),
					new BoxedLongPropertyTypeDescriptor(),
					new BoxedBooleanPropertyTypeDescriptor(),
					new PrimitiveIntegerPropertyTypeDescriptor(),
					new PrimitiveLongPropertyTypeDescriptor(),
					new PrimitiveBooleanPropertyTypeDescriptor(),
					new EnumPropertyTypeDescriptor(),
					new InstantPropertyTypeDescriptor(),
					new LocalDatePropertyTypeDescriptor(),
					new JavaUtilDatePropertyTypeDescriptor()
			) );
		}
		return all;
	}

	private final Class<V> javaType;

	protected PropertyTypeDescriptor(Class<V> javaType) {
		this.javaType = javaType;
	}

	public final Class<V> getJavaType() {
		return javaType;
	}

	public abstract Optional<DefaultIdentifierBridgeExpectations<V>> getDefaultIdentifierBridgeExpectations();

	public abstract Optional<DefaultValueBridgeExpectations<V, ?>> getDefaultValueBridgeExpectations();

}
