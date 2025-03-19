/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.writer.impl;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.PojoValueBridgeDocumentValueConverter;
import org.hibernate.search.metamodel.processor.annotation.processing.impl.AbstractProcessorFieldAnnotationProcessor;

public class MetamodelClassWriter {

	public static class Builder {
		private final TraitReferenceMapping traitReferenceMapping;
		private final Map<String, ValueFieldReferenceDetails> valueFieldReferenceDetails;
		private final MetamodelNamesFormatter metamodelNamesFormatter;

		private final String packageName;
		private final String className;
		private final String scopeTypeName;
		private final TreeSet<RegularProperty> regularProperties;
		private final TreeSet<ObjectField> objectProperties;
		private final int depth;

		public Builder(MetamodelNamesFormatter metamodelNamesFormatter, String packageName, String className) {
			this( metamodelNamesFormatter, packageName, className,
					metamodelClassName( metamodelNamesFormatter, packageName, className ),
					TraitReferenceMapping.instance(), new TreeMap<>(), 1 );
		}

		private Builder(MetamodelNamesFormatter metamodelNamesFormatter, String packageName, String className,
				String scopeTypeName, TraitReferenceMapping traitReferenceMapping,
				Map<String, ValueFieldReferenceDetails> valueFieldReferenceDetails, int depth) {
			this.traitReferenceMapping = traitReferenceMapping;
			this.valueFieldReferenceDetails = valueFieldReferenceDetails;
			this.metamodelNamesFormatter = metamodelNamesFormatter;

			this.packageName = packageName;
			this.className = className;
			this.scopeTypeName = scopeTypeName;
			this.regularProperties = new TreeSet<>();
			this.objectProperties = new TreeSet<>();
			this.depth = depth;
		}

		public void addProperty(IndexValueFieldDescriptor valueField) {
			IndexValueFieldTypeDescriptor type = valueField.type();
			if ( type instanceof SearchIndexValueFieldTypeContext<?, ?, ?> context ) {
				regularProperties.add( new RegularProperty(
						valueField.relativeName(),
						valueField.absolutePath(),
						typeFromDslConverter( context.mappingDslConverter() ),
						typeFromProjectionConverter( context.mappingProjectionConverter() ),
						typeFromDslConverter( context.indexDslConverter() ),
						typeFromDslConverter( context.rawDslConverter() ),
						fromTraits( type.traits() )
				) );
			}
			else {
				// TODO log message
			}
		}

		private String typeFromDslConverter(DslConverter<?, ?> converter) {
			return typeFromConverter( converter.delegate(), converter.valueType() );
		}

		private String typeFromProjectionConverter(ProjectionConverter<?, ?> converter) {
			return typeFromConverter( converter.delegate(), converter.valueType() );
		}

		private String typeFromConverter(Object delegate, Class<?> valueType) {
			if ( delegate instanceof PojoValueBridgeDocumentValueConverter<?, ?> pvbdc
					&& pvbdc.bridge() instanceof AbstractProcessorFieldAnnotationProcessor.ProcessorEnumValueBridge bridge ) {
				return bridge.valueType().toString();
			}
			return typeToString( valueType );
		}

		public void addProperty(IndexObjectFieldDescriptor objectField) {
			String name = objectField.relativeName();
			Builder builder = new Builder( metamodelNamesFormatter, "", metamodelClassName() + name, scopeTypeName,
					traitReferenceMapping, valueFieldReferenceDetails, depth + 1 );
			objectProperties.add( new ObjectField( name, objectField.absolutePath(), objectField.type().nested(), builder ) );

			for ( IndexFieldDescriptor child : objectField.staticChildren() ) {
				if ( child.isValueField() ) {
					builder.addProperty( child.toValueField() );
				}
				else {
					builder.addProperty( child.toObjectField() );
				}
			}
		}

		// todo: remove this once types are correctly collected from type elements instead:
		private String typeToString(Class<?> type) {
			if ( type.isArray() ) {
				return type.getComponentType().getName() + "[]";
			}
			else {
				return type.getName();
			}
		}

		private ValueFieldReferenceDetails fromTraits(Collection<String> traits) {
			Set<TraitReferenceDetails> details = new TreeSet<>();
			for ( String trait : traits ) {
				details.add( traitReferenceMapping.reference( trait ) );
			}
			String key = details.stream().map( TraitReferenceDetails::implementationLabel ).collect( Collectors.joining() );

			return valueFieldReferenceDetails.computeIfAbsent( key, k -> createValueFieldReferenceDetails( details ) );
		}

		private ValueFieldReferenceDetails createValueFieldReferenceDetails(Set<TraitReferenceDetails> details) {
			return new ValueFieldReferenceDetails( TypedFieldReferenceDetails.of( details ) );
		}

		public String formatted() {
			String metamodelClassName = metamodelClassName();
			return String.format( Locale.ROOT,
					"""
							%s
							@javax.annotation.processing.Generated(value = "org.hibernate.search.metamodel.processor.HibernateSearchMetamodelProcessor")
							public final class %s implements
								org.hibernate.search.engine.search.reference.RootReferenceScope<%s, %s> {

								public static final %s %s = new %s();

								public final %s;

								private %s() {
									// simple value field references:
									%s
									// various object field references:
									%s
								}

								@Override
								public Class<%s> rootReferenceType() {
									return %s.class;
								}

							%s

							%s
							}
							""",
					packageName.isEmpty() ? "" : "package " + packageName + ";\n",
					metamodelClassName,
					metamodelClassName,
					className,
					metamodelClassName,
					metamodelNamesFormatter.formatIndexFieldName( metamodelClassName ),
					metamodelClassName,
					Stream.concat(
							regularProperties.stream().map( p -> p.asProperty( metamodelClassName ) ),
							objectProperties.stream().map( p -> p.asProperty( metamodelClassName ) ) )
							.collect( Collectors.joining( ";\n\tpublic final " ) ),
					metamodelClassName,
					regularProperties.stream().map( p -> p.asSetInConstructor( metamodelClassName ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					objectProperties.stream().map( p -> p.asSetInConstructor( metamodelClassName ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					metamodelClassName,
					metamodelClassName,
					valueFieldReferenceDetails.values().stream()
							.map( ValueFieldReferenceDetails::formattedWithTypedField )
							.collect( Collectors.joining( "\n\n" ) )
							.replaceAll( "(?m)^", indent() ),
					objectProperties.stream()
							.map( f -> f.formatted( scopeTypeName ) )
							.collect( Collectors.joining( "\n\n" ) )
							.replaceAll( "(?m)^", indent() )
			);
		}

		private String indent() {
			return "\t".repeat( depth );
		}

		public String metamodelClassName() {
			return metamodelClassName( metamodelNamesFormatter, packageName, className );
		}

		private static String metamodelClassName(MetamodelNamesFormatter metamodelNamesFormatter, String packageName,
				String className) {
			String classSimpleName;
			if ( className.contains( "." ) ) {
				classSimpleName = className.substring( packageName.length() + 1 ).replace( '.', '_' );
			}
			else {
				classSimpleName = className;
			}
			return metamodelNamesFormatter.formatMetamodelClassName( classSimpleName );
		}

		public CharSequence fqcn() {
			return packageName.isEmpty() ? metamodelClassName() : packageName + "." + metamodelClassName();
		}
	}

	private record RegularProperty( String name,
									String path,
									String inputType,
									String outputType,
									String indexType,
									String rawType,
									ValueFieldReferenceDetails valueFieldReference)
			implements Comparable<RegularProperty> {

		@Override
		public int compareTo(RegularProperty o) {
			return name.compareTo( o.name );
		}

		public String asProperty(String scopeType) {
			return valueFieldReference().asType( scopeType, inputType, outputType, indexType, rawType ) + " " + name;
		}

		public String asSetInConstructor(String scopeType) {
			return "this." + name + " = "
					+ valueFieldReference.constructorCall( path, scopeType, inputType, outputType, indexType, rawType );
		}
	}

	private record ObjectField(String name, String path, boolean nested, Builder builder) implements Comparable<ObjectField> {
		public String asProperty(String scopeType) {
			return builder.metamodelClassName() + " " + name;
		}

		public String asSetInConstructor(String scopeType) {
			return "this." + name + " = new " + builder.metamodelClassName() + "();";
		}

		public String formatted(String scopeType) {
			String metamodelClassName = builder.metamodelClassName();
			return String.format( Locale.ROOT, """
					public static class %s implements %s<%s> {

						public final %s;

						private %s() {
							// simple value field references:
							%s
							// various object field references:
							%s
						}

						@Override
						public String absolutePath() {
							return "%s";
						}

						@Override
						public Class<%s> scopeRootType() {
							return %s.class;
						}

					%s
					}
					""",
					metamodelClassName,
					objectReferenceClass(),
					scopeType,
					Stream.concat(
							builder.regularProperties.stream().map( p -> p.asProperty( scopeType ) ),
							builder.objectProperties.stream().map( p -> p.asProperty( scopeType ) ) )
							.collect( Collectors.joining( ";\n\tpublic final " ) ),
					metamodelClassName,
					builder.regularProperties.stream().map( p -> p.asSetInConstructor( scopeType ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					builder.objectProperties.stream().map( p -> p.asSetInConstructor( scopeType ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					path,
					scopeType,
					scopeType,
					builder.objectProperties.stream()
							.map( f -> f.formatted( builder.scopeTypeName ) )
							.collect( Collectors.joining( "\n\n" ) )
							.replaceAll( "(?m)^", "\t" )
			);
		}

		private String objectReferenceClass() {
			if ( nested ) {
				return "org.hibernate.search.engine.search.reference.object.NestedFieldReference";
			}
			else {
				return "org.hibernate.search.engine.search.reference.object.FlattenedFieldReference";
			}
		}

		@Override
		public int compareTo(ObjectField o) {
			return name.compareTo( o.name );
		}
	}

}
