/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model.impl;

import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class BuiltInBridgeResolverTypes {

	private BuiltInBridgeResolverTypes() {
	}

	// TODO: need test that types are not missing
	private static Set<String> TYPES = Set.of(
			"java.lang.String",
			"java.lang.Character",
			"java.lang.Boolean",
			"java.lang.Byte",
			"java.lang.Short",
			"java.lang.Integer",
			"java.lang.Long",
			"java.lang.Float",
			"java.lang.Double",

			// TODO: handle enums somehow:
			// strictSubTypesOf( Enum.class )

			"java.math.BigInteger",
			"java.math.BigDecimal",

			"java.time.LocalDate",
			"java.time.Instant",
			"java.time.LocalDateTime",
			"java.time.LocalTime",
			"java.time.ZonedDateTime",
			"java.time.Year",
			"java.time.YearMonth",
			"java.time.MonthDay",
			"java.time.OffsetDateTime",
			"java.time.OffsetTime",
			"java.time.ZoneOffset",
			"java.time.ZoneId",
			"java.time.Period",
			"java.time.Duration",

			"java.util.UUID",
			"java.util.Date",
			"java.util.Calendar",

			"java.sql.Date",
			"java.sql.Timestamp",
			"java.sql.Time",

			"java.net.URI",
			"java.net.URL",

			// TODO: handle geo pints
			// subTypesOf( GeoPoint.class )
			"org.hibernate.search.engine.spatial.GeoPoint"

	// arrays for vector fields:
	// primitive types are handled differently
	);

	private static Set<String> CONTAINERS = Set.of(
			"java.util.List",
			"java.util.Set"
	);

	public static boolean isBuiltInType(String typeName) {
		return TYPES.contains( typeName );
	}

	public static boolean isContainer(TypeMirror mirror, Types types) {
		if ( mirror == null || mirror.getKind() == TypeKind.NONE ) {
			return false;
		}
		TypeElement element = (TypeElement) types.asElement( mirror );
		if ( CONTAINERS.contains( element.getQualifiedName().toString() ) ) {
			return true;
		}
		if ( isContainer( element.getSuperclass(), types ) ) {
			return true;
		}
		for ( TypeMirror i : element.getInterfaces() ) {
			if ( isContainer( i, types ) ) {
				return true;
			}
		}

		return false;
	}

	public static Optional<Class<?>> loadableType(TypeMirror propertyType, Types types) {
		try {
			if ( propertyType instanceof ArrayType arrayType ) {
				TypeMirror componentType = arrayType.getComponentType();
				Class<?> componentTypeClass = null;
				if ( componentType.getKind().isPrimitive() ) {
					switch ( componentType.getKind() ) {
						case BOOLEAN -> componentTypeClass = boolean.class;
						case BYTE -> componentTypeClass = byte.class;
						case SHORT -> componentTypeClass = short.class;
						case INT -> componentTypeClass = int.class;
						case LONG -> componentTypeClass = long.class;
						case CHAR -> componentTypeClass = char.class;
						case FLOAT -> componentTypeClass = float.class;
						case DOUBLE -> componentTypeClass = double.class;
						default -> throw new IllegalStateException( "Unsupported primitive type: " + componentType.getKind() );
					}
				}
				else if ( isBuiltInType( componentType.toString() ) ) {
					componentTypeClass = Class.forName( componentType.toString() );
				}
				if ( componentTypeClass != null ) {
					return Optional.of( componentTypeClass.arrayType() );
				}
				else {
					return Optional.empty();
				}
			}

			String typeName = propertyType.toString();
			return isBuiltInType( typeName )
					? Optional.of( Class.forName( typeName ) )
					: Optional.empty();
		}
		catch (ClassNotFoundException e) {
			// TODO: ...
			throw new RuntimeException( e );
		}
	}
}
