/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.mapping.impl.AdditionalMappingAnnotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import jakarta.persistence.AccessType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Index;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.search.util.common.AssertionFailure;

public class AdditionalMappingBuilder {

	private final MetadataBuildingContext buildingContext;
	private final Class<?> type;
	private final String name;
	private final TableAnnotation tableAnnotation = new TableAnnotation();
	private final List<BiConsumer<ModelsContext, MutableClassDetails>> contributors = new ArrayList<>();

	public AdditionalMappingBuilder(MetadataBuildingContext buildingContext, Class<?> type, String name) {
		this.buildingContext = buildingContext;
		this.type = type;
		this.name = name;
	}

	public AdditionalMappingBuilder table(String schema, String catalog, String table) {
		tableAnnotation.schema( schema );
		tableAnnotation.catalog( catalog );
		tableAnnotation.name( table );
		return this;
	}

	public AdditionalMappingBuilder index(String name) {
		return index( name, name );
	}

	public AdditionalMappingBuilder index(String name, String columns) {
		final IndexAnnotation indexUsage = new IndexAnnotation();
		indexUsage.name( name );
		indexUsage.columnList( columns );

		final Index[] existing = tableAnnotation.indexes();
		final Index[] updated = new Index[existing.length + 1];
		System.arraycopy( existing, 0, updated, 0, existing.length );
		updated[existing.length] = indexUsage;
		tableAnnotation.indexes( updated );
		return this;
	}

	public AdditionalMappingBuilder attribute(String name, Integer length, Boolean nullable) {
		return attribute( name, length, nullable, null );
	}

	public AdditionalMappingBuilder attribute(String name, Integer length, Boolean nullable, Integer type) {
		createAttribute( name, length, nullable, type );
		return this;
	}

	public AdditionalMappingBuilder tenantId(String name) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			field.addAnnotationUsage( new TenantIdAnnotation() );
		} );
		return this;
	}

	public AdditionalMappingBuilder enumAttribute(String name, Integer length, Boolean nullable) {
		createAttribute( name, length, nullable );
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			final EnumeratedAnnotation enumeratedUsage = new EnumeratedAnnotation();
			enumeratedUsage.value( EnumType.STRING );
			field.addAnnotationUsage( enumeratedUsage );
		} );
		return this;
	}

	public AdditionalMappingBuilder id(Integer type, String strategy) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( "id" );

			field.addAnnotationUsage( new IdAnnotation() );

			final UuidGeneratorAnnotation uuidGeneratorUsage = new UuidGeneratorAnnotation();
			uuidGeneratorUsage.style( UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) ) );
			field.addAnnotationUsage( uuidGeneratorUsage );

			if ( type != null ) {
				final JdbcTypeCodeAnnotation jdbcTypeCodeUsage = new JdbcTypeCodeAnnotation();
				jdbcTypeCodeUsage.value( type );
				field.addAnnotationUsage( jdbcTypeCodeUsage );
			}
		} );

		return this;
	}

	public ClassDetails build() {
		final ModelsContext context = buildingContext.getBootstrapContext().getModelsContext();
		if ( context.getClassDetailsRegistry()
				.resolveClassDetails( type.getName() ) instanceof MutableClassDetails classDetails ) {
			EntityAnnotation entityUsage = new EntityAnnotation();
			entityUsage.name( name );
			classDetails.addAnnotationUsage( entityUsage );

			AccessAnnotation accessUsage = new AccessAnnotation();
			accessUsage.value( AccessType.FIELD );
			classDetails.addAnnotationUsage( accessUsage );
			classDetails.addAnnotationUsage( tableAnnotation );

			for ( BiConsumer<ModelsContext, MutableClassDetails> contributor : contributors ) {
				contributor.accept( context, classDetails );
			}

			return classDetails;
		}
		else {
			throw new AssertionFailure( "Cannot build mutable class details for " + type );
		}
	}

	private void createAttribute(String name, Integer size, boolean nullable) {
		createAttribute( name, size, nullable, null );
	}

	private void createAttribute(String name, Integer length, boolean nullable, Integer type) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			ColumnAnnotation columnUsage = new ColumnAnnotation();

			columnUsage.name( name );
			columnUsage.nullable( nullable );
			if ( length != null ) {
				columnUsage.length( length );
			}

			field.addAnnotationUsage( columnUsage );

			if ( type != null ) {
				final JdbcTypeCodeAnnotation jdbcTypeCodeUsage = new JdbcTypeCodeAnnotation();
				jdbcTypeCodeUsage.value( type );
				field.addAnnotationUsage( jdbcTypeCodeUsage );
			}
		} );
	}
}
