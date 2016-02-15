/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.dto.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.genericjpa.annotations.DtoField;
import org.hibernate.search.genericjpa.annotations.DtoFields;
import org.hibernate.search.genericjpa.annotations.DtoOverEntity;

public class DtoDescriptorImpl implements DtoDescriptor {

	@Override
	public DtoDescription getDtoDescription(Class<?> clazz) {
		final Map<String, Set<DtoDescription.FieldDescription>> fieldDescriptionsForProfile = new HashMap<>();
		DtoOverEntity[] dtoOverEntity = clazz.getAnnotationsByType( DtoOverEntity.class );
		if ( dtoOverEntity.length != 1 ) {
			throw new IllegalArgumentException( "clazz must specify exactly one " + "DtoOverEntity annotation at a class level" );
		}
		java.lang.reflect.Field[] declared = clazz.getDeclaredFields();
		Arrays.asList( declared ).forEach(
				(field) -> {
					// should be accessible :)
					field.setAccessible( true );
					List<DtoField> annotations = new ArrayList<>();
					{
						DtoFields dtoFields = field.getAnnotation( DtoFields.class );
						if ( dtoFields != null ) {
							annotations.addAll( Arrays.asList( dtoFields.value() ) );
						}
						else {
							DtoField dtoField = field.getAnnotation( DtoField.class );
							if ( dtoField != null ) {
								annotations.add( dtoField );
							}
						}
					}
					annotations.forEach(
							(annotation) -> {
								String profileName = annotation.profileName();
								String fieldName = annotation.fieldName();
								if ( fieldName.equals( DtoDescription.DEFAULT_FIELD_NAME ) ) {
									// if we want to support
									// hierarchies at any time
									// in the future we have to
									// change this!
									fieldName = field.getName();
								}
								Set<DtoDescription.FieldDescription> fieldDescriptions = fieldDescriptionsForProfile.computeIfAbsent(
										profileName, (key) -> {
											return new HashSet<>();
										}
								);
								DtoDescription.FieldDescription fieldDesc = new DtoDescription.FieldDescription( fieldName, field );
								if ( fieldDescriptions.contains( fieldDesc ) ) {
									throw new IllegalArgumentException( "profile " + profileName + " already has a field to project from for " + field );
								}
								fieldDescriptions.add( fieldDesc );
							}
					);
				}
		);
		if ( fieldDescriptionsForProfile.isEmpty() ) {
			throw new IllegalArgumentException( "no DtoField(s) found! The passed class is no annotated DTO" );
		}
		return new DtoDescription( clazz, dtoOverEntity[0].entityClass(), fieldDescriptionsForProfile );
	}

}
