/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import static org.hibernate.models.internal.AnnotationHelper.createOrmDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import jakarta.persistence.AccessType;
import jakarta.persistence.EnumType;

import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.internal.jdk.JdkBuilders;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
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
			classDetails.applyAnnotationUsage(
					JpaAnnotations.TABLE,
					(tableUsage) -> {
						tableUsage.setAttributeValue( "schema", schema );
						tableUsage.setAttributeValue( "catalog", catalog );
						tableUsage.setAttributeValue( "name", table );
					},
					context
			);
		} );
		return this;
	}

	public AdditionalMappingBuilder index(String name) {
		return index( name, name );
	}

	public AdditionalMappingBuilder index(String name, String columns) {
		contributors.add( (context, classDetails) -> {
			classDetails.applyAnnotationUsage(
					JpaAnnotations.INDEX,
					(indexUsage) -> {
						indexUsage.setAttributeValue( "name", name );
						indexUsage.setAttributeValue( "columnList", columns );
					},
					context
			);
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
			field.applyAnnotationUsage(
					JpaAnnotations.ENUMERATED,
					(entityUsage) -> entityUsage.setAttributeValue( "value", EnumType.STRING ),
					context
			);
		} );
		return this;
	}

	public AdditionalMappingBuilder id(Integer type, String strategy) {
		contributors.add( (context, classDetails) -> {
			final MutableMemberDetails field = (MutableMemberDetails) classDetails.findFieldByName( "id" );

			field.applyAnnotationUsage( JpaAnnotations.ID, context );

			field.applyAnnotationUsage(
					UUID_GENERATOR,
					(uuidGeneratorUsage) -> uuidGeneratorUsage.setAttributeValue( "style",
							UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) ) ),
					context
			);
			if ( type != null ) {
				field.applyAnnotationUsage(
						HibernateAnnotations.JDBC_TYPE_CODE,
						(jdbcTypeCodeUsage) -> jdbcTypeCodeUsage.setAttributeValue( "value", type ),
						context
				);
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

					classDetails.applyAnnotationUsage(
							JpaAnnotations.ENTITY,
							(entityUsage) -> entityUsage.setAttributeValue( "name", name ),
							context
					);
					classDetails.applyAnnotationUsage(
							JpaAnnotations.ACCESS,
							(accessUsage) -> accessUsage.setAttributeValue( "value", AccessType.FIELD ),
							context
					);

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
			field.applyAnnotationUsage(
					JpaAnnotations.COLUMN,
					(columnUsage) -> {
						columnUsage.setAttributeValue( "name", name );
						columnUsage.setAttributeValue( "nullable", nullable );
						if ( length != null ) {
							columnUsage.setAttributeValue( "length", length );
						}
					},
					context
			);
			if ( type != null ) {
				field.applyAnnotationUsage(
						HibernateAnnotations.JDBC_TYPE_CODE,
						(jdbcTypeCodeUsage) -> jdbcTypeCodeUsage.setAttributeValue( "value", type ),
						context
				);
			}
		} );
	}
}
