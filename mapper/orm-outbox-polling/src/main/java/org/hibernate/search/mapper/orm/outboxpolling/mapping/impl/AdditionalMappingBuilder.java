/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import jakarta.persistence.AccessType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Index;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EnumeratedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IndexJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JdbcTypeCodeAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.UuidGeneratorAnnotation;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.jdk.JdkBuilders;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

public class AdditionalMappingBuilder {

	private final MetadataBuildingContext buildingContext;
	private final Class<?> type;
	private final String name;
	private final List<BiConsumer<SourceModelBuildingContext, MutableClassDetails>> contributors = new ArrayList<>();

	public AdditionalMappingBuilder(MetadataBuildingContext buildingContext, Class<?> type, String name) {
		this.buildingContext = buildingContext;
		this.type = type;
		this.name = name;
	}

	public AdditionalMappingBuilder table(String schema, String catalog, String table) {
		contributors.add( (context, classDetails) -> {
			TableJpaAnnotation tableUsage = (TableJpaAnnotation) classDetails.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					context
			);
			tableUsage.schema( schema );
			tableUsage.catalog( catalog );
			tableUsage.name( table );
		} );
		return this;
	}

	public AdditionalMappingBuilder index(String name) {
		return index( name, name );
	}

	public AdditionalMappingBuilder index(String name, String columns) {
		contributors.add( (context, classDetails) -> {
			TableJpaAnnotation tableUsage = (TableJpaAnnotation) classDetails.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					context
			);

			IndexJpaAnnotation indexUsage = JpaAnnotations.INDEX.createUsage( context );
			indexUsage.name( name );
			indexUsage.columnList( columns );

			tableUsage.indexes( new Index[] { indexUsage } );
		} );
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
			field.applyAnnotationUsage( HibernateAnnotations.TENANT_ID, context );
		} );
		return this;
	}

	public AdditionalMappingBuilder enumAttribute(String name, Integer length, Boolean nullable) {
		createAttribute( name, length, nullable );
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			EnumeratedJpaAnnotation entityUsage = (EnumeratedJpaAnnotation) field.applyAnnotationUsage(
					JpaAnnotations.ENUMERATED,
					context
			);
			entityUsage.value( EnumType.STRING );
		} );
		return this;
	}

	public AdditionalMappingBuilder id(Integer type, String strategy) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( "id" );

			field.applyAnnotationUsage( JpaAnnotations.ID, context );

			UuidGeneratorAnnotation uuidGeneratorUsage = (UuidGeneratorAnnotation) field.applyAnnotationUsage(
					HibernateAnnotations.UUID_GENERATOR,
					context
			);
			uuidGeneratorUsage.style(
					UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) )
			);
			if ( type != null ) {
				JdbcTypeCodeAnnotation jdbcTypeCodeUsage = (JdbcTypeCodeAnnotation) field.applyAnnotationUsage(
						HibernateAnnotations.JDBC_TYPE_CODE,
						context
				);
				jdbcTypeCodeUsage.value( type );
			}
		} );

		return this;
	}

	public ClassDetails build() {
		SourceModelBuildingContext context = buildingContext.getMetadataCollector()
				.getSourceModelBuildingContext();
		final MutableClassDetails classDetails = JdkBuilders.buildClassDetailsStatic(
				type,
				context
		);

		EntityJpaAnnotation entityUsage = (EntityJpaAnnotation) classDetails.applyAnnotationUsage(
				JpaAnnotations.ENTITY,
				context
		);
		entityUsage.name( name );
		AccessJpaAnnotation accessUsage = (AccessJpaAnnotation) classDetails.applyAnnotationUsage(
				JpaAnnotations.ACCESS,
				context
		);
		accessUsage.value( AccessType.FIELD );

		for ( BiConsumer<SourceModelBuildingContext, MutableClassDetails> contributor : contributors ) {
			contributor.accept( context, classDetails );
		}

		context.getClassDetailsRegistry()
				.as( MutableClassDetailsRegistry.class )
				.addClassDetails( type.getName(), classDetails );

		return classDetails;
	}

	private void createAttribute(String name, Integer size, boolean nullable) {
		createAttribute( name, size, nullable, null );
	}

	private void createAttribute(String name, Integer length, boolean nullable, Integer type) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			ColumnJpaAnnotation columnUsage = (ColumnJpaAnnotation) field.applyAnnotationUsage(
					JpaAnnotations.COLUMN,
					context
			);

			columnUsage.name( name );
			columnUsage.nullable( nullable );
			if ( length != null ) {
				columnUsage.length( length );
			}

			if ( type != null ) {
				JdbcTypeCodeAnnotation jdbcTypeCodeUsage =
						(JdbcTypeCodeAnnotation) field.applyAnnotationUsage( HibernateAnnotations.JDBC_TYPE_CODE, context );
				jdbcTypeCodeUsage.value( type );
			}
		} );
	}
}
