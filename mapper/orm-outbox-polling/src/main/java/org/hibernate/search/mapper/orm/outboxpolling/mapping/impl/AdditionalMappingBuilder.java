/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import static org.hibernate.models.internal.AnnotationHelper.createOrmDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.internal.jdk.JdkBuilders;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

public class AdditionalMappingBuilder {

	private static final AnnotationDescriptor<TenantId> TENANT_ID = createOrmDescriptor( TenantId.class );
	private static final AnnotationDescriptor<UuidGenerator> UUID_GENERATOR = createOrmDescriptor( UuidGenerator.class );

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
			MutableAnnotationUsage<Table> tableUsage = classDetails.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					context
			);
			tableUsage.setAttributeValue( "schema", schema );
			tableUsage.setAttributeValue( "catalog", catalog );
			tableUsage.setAttributeValue( "name", table );
		} );
		return this;
	}

	public AdditionalMappingBuilder index(String name) {
		return index( name, name );
	}

	public AdditionalMappingBuilder index(String name, String columns) {
		contributors.add( (context, classDetails) -> {
			MutableAnnotationUsage<Table> tableUsage = classDetails.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					context
			);

			MutableAnnotationUsage<Index> indexUsage = JpaAnnotations.INDEX.createUsage( context );
			indexUsage.setAttributeValue( "name", name );
			indexUsage.setAttributeValue( "columnList", columns );

			tableUsage.setAttributeValue( "indexes", List.of( indexUsage ) );
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
			field.applyAnnotationUsage( TENANT_ID, context );
		} );
		return this;
	}

	public AdditionalMappingBuilder enumAttribute(String name, Integer length, Boolean nullable) {
		createAttribute( name, length, nullable );
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			MutableAnnotationUsage<Enumerated> entityUsage = field.applyAnnotationUsage(
					JpaAnnotations.ENUMERATED,
					context
			);
			entityUsage.setAttributeValue( "value", EnumType.STRING );
		} );
		return this;
	}

	public AdditionalMappingBuilder id(Integer type, String strategy) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( "id" );

			field.applyAnnotationUsage( JpaAnnotations.ID, context );

			MutableAnnotationUsage<UuidGenerator> uuidGeneratorUsage = field.applyAnnotationUsage(
					UUID_GENERATOR,
					context
			);
			uuidGeneratorUsage.setAttributeValue(
					"style",
					UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) )
			);
			if ( type != null ) {
				MutableAnnotationUsage<JdbcTypeCode> jdbcTypeCodeUsage = field.applyAnnotationUsage(
						HibernateAnnotations.JDBC_TYPE_CODE,
						context
				);
				jdbcTypeCodeUsage.setAttributeValue( "value", type );
			}
		} );

		return this;
	}

	public ClassDetails build() {
		return buildingContext.getMetadataCollector()
				.getSourceModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( type.getName(), (n, context) -> {
					final MutableClassDetails classDetails = JdkBuilders.buildClassDetailsStatic(
							type,
							context
					);

					MutableAnnotationUsage<Entity> entityUsage = classDetails.applyAnnotationUsage(
							JpaAnnotations.ENTITY,
							context
					);
					entityUsage.setAttributeValue( "name", name );
					MutableAnnotationUsage<Access> accessUsage = classDetails.applyAnnotationUsage(
							JpaAnnotations.ACCESS,
							context
					);
					accessUsage.setAttributeValue( "value", AccessType.FIELD );

					for ( BiConsumer<SourceModelBuildingContext, MutableClassDetails> contributor : contributors ) {
						contributor.accept( context, classDetails );
					}

					return classDetails;
				} );
	}

	private void createAttribute(String name, Integer size, boolean nullable) {
		createAttribute( name, size, nullable, null );
	}

	private void createAttribute(String name, Integer length, boolean nullable, Integer type) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( name );
			MutableAnnotationUsage<Column> columnUsage = field.applyAnnotationUsage(
					JpaAnnotations.COLUMN,
					context
			);

			columnUsage.setAttributeValue( "name", name );
			columnUsage.setAttributeValue( "nullable", nullable );
			if ( length != null ) {
				columnUsage.setAttributeValue( "length", length );
			}

			if ( type != null ) {
				MutableAnnotationUsage<JdbcTypeCode> jdbcTypeCodeUsage =
						field.applyAnnotationUsage( HibernateAnnotations.JDBC_TYPE_CODE, context );
				jdbcTypeCodeUsage.setAttributeValue( "value", type );
			}
		} );
	}
}
