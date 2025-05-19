/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.writer.impl;

import static org.hibernate.search.processor.HibernateSearchProcessorSettings.Configuration;

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
import org.hibernate.search.processor.model.impl.HibernateSearchProcessorEnum;
import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

@SuppressJQAssistant(reason = "Need to use an impl clss for the value converter to get to the type element")
public class MetamodelClassWriter {
	private final TraitReferenceMapping traitReferenceMapping;
	private final Map<String, ValueFieldReferenceDetails> valueFieldReferenceDetails;
	private final Configuration configuration;
	private final MetamodelNamesFormatter metamodelNamesFormatter;

	private final boolean ormMapperPresent;
	private final String packageName;
	private final String className;
	private final String scopeTypeName;
	private final TreeSet<RegularProperty> regularProperties;
	private final TreeSet<ObjectField> objectProperties;
	private final int depth;

	public MetamodelClassWriter(boolean ormMapperPresent, Configuration configuration,
			MetamodelNamesFormatter metamodelNamesFormatter,
			String packageName, String className) {
		this( ormMapperPresent, configuration, metamodelNamesFormatter, packageName, className,
				metamodelClassName( metamodelNamesFormatter, packageName, className ),
				TraitReferenceMapping.instance(), new TreeMap<>(), 1 );
	}

	private MetamodelClassWriter(boolean ormMapperPresent, Configuration configuration,
			MetamodelNamesFormatter metamodelNamesFormatter,
			String packageName, String className,
			String scopeTypeName, TraitReferenceMapping traitReferenceMapping,
			Map<String, ValueFieldReferenceDetails> valueFieldReferenceDetails, int depth) {
		this.traitReferenceMapping = traitReferenceMapping;
		this.valueFieldReferenceDetails = valueFieldReferenceDetails;
		this.configuration = configuration;
		this.metamodelNamesFormatter = metamodelNamesFormatter;

		this.ormMapperPresent = ormMapperPresent;
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
	}

	private String typeFromDslConverter(DslConverter<?, ?> converter) {
		return typeFromConverter( converter.delegate(), converter.valueType() );
	}

	private String typeFromProjectionConverter(ProjectionConverter<?, ?> converter) {
		return typeFromConverter( converter.delegate(), converter.valueType() );
	}

	private String typeFromConverter(Object delegate, Class<?> valueType) {
		if ( delegate instanceof PojoValueBridgeDocumentValueConverter<?, ?> pvbdc
				&& pvbdc.bridge() instanceof HibernateSearchProcessorEnum.Bridge bridge ) {
			return bridge.valueType();
		}
		return typeToString( valueType );
	}

	public void addProperty(IndexObjectFieldDescriptor objectField) {
		String name = objectField.relativeName();
		MetamodelClassWriter writer =
				new MetamodelClassWriter( ormMapperPresent, configuration, metamodelNamesFormatter, "",
						metamodelClassName() + name,
						scopeTypeName,
						traitReferenceMapping, valueFieldReferenceDetails, depth + 1 );
		objectProperties.add( new ObjectField( name, objectField.absolutePath(), objectField.type().nested(), writer ) );

		for ( IndexFieldDescriptor child : objectField.staticChildren() ) {
			if ( child.isValueField() ) {
				writer.addProperty( child.toValueField() );
			}
			else {
				writer.addProperty( child.toObjectField() );
			}
		}
	}

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
		return String.format(
				Locale.ROOT,
				"""
						%s
						%s
						public final class %s implements
							%s<%s, %s> {

							public static final %s %s = new %s();

							%s
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

						%s
						}
						""",
				packageName.isEmpty() ? "" : "package " + packageName + ";\n",
				configuration.formattedGeneratedAnnotation(),
				metamodelClassName,
				scopeInterfaceType(),
				metamodelClassName,
				className,
				metamodelClassName,
				metamodelNamesFormatter.formatIndexFieldName( metamodelClassName ),
				metamodelClassName,
				getFieldReferences( metamodelClassName ),
				metamodelClassName,
				regularProperties.stream().map( p -> p.asSetInConstructor( metamodelClassName ) )
						.collect( Collectors.joining( "\n\t\t" ) ),
				objectProperties.stream().map( p -> p.asSetInConstructor( metamodelClassName ) )
						.collect( Collectors.joining( "\n\t\t" ) ),
				metamodelClassName,
				metamodelClassName,
				scopeMethod( metamodelClassName, className ),
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

	private String getFieldReferences(String metamodelClassName) {
		if ( regularProperties.isEmpty() && objectProperties.isEmpty() ) {
			return "";
		}
		return Stream.concat(
				regularProperties.stream().map( p -> p.asProperty( metamodelClassName ) ),
				objectProperties.stream().map( p -> p.asProperty( metamodelClassName ) )
		).collect( Collectors.joining( ";\n\tpublic final ", "public final ", ";\n" ) );
	}

	private String scopeInterfaceType() {
		return ormMapperPresent
				? "org.hibernate.search.mapper.orm.scope.HibernateOrmRootReferenceScope"
				: "org.hibernate.search.mapper.pojo.standalone.scope.StandalonePojoRootReferenceScope";
	}

	private String scopeMethod(String metamodelClassName, String entityName) {
		return String.format( Locale.ROOT, ormMapperPresent
				? """
							@Override
							public org.hibernate.search.mapper.orm.scope.TypedSearchScope<%s, %s> scope(org.hibernate.search.mapper.orm.scope.SearchScopeProvider scopeProvider) {
								return scopeProvider.typedScope( %s.class, java.util.List.of( %s.class ) );
							}
						"""
				: """
							@Override
							public org.hibernate.search.mapper.pojo.standalone.scope.TypedSearchScope<%s, %s> scope(org.hibernate.search.mapper.pojo.standalone.scope.SearchScopeProvider scopeProvider) {
								return scopeProvider.typedScope( %s.class, java.util.List.of( %s.class ) );
							}
						""",
				metamodelClassName, entityName, metamodelClassName, entityName );
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

	private record ObjectField(String name, String path, boolean nested, MetamodelClassWriter writer)
			implements Comparable<ObjectField> {
		public String asProperty(String scopeType) {
			return writer.metamodelClassName() + " " + name;
		}

		public String asSetInConstructor(String scopeType) {
			return "this." + name + " = new " + writer.metamodelClassName() + "();";
		}

		public String formatted(String scopeType) {
			String metamodelClassName = writer.metamodelClassName();
			return String.format(
					Locale.ROOT, """
							public static class %s implements %s<%s> {

								%s
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
					getFieldReferences( scopeType ),
					metamodelClassName,
					writer.regularProperties.stream().map( p -> p.asSetInConstructor( scopeType ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					writer.objectProperties.stream().map( p -> p.asSetInConstructor( scopeType ) )
							.collect( Collectors.joining( "\n\t\t" ) ),
					path,
					scopeType,
					scopeType,
					writer.objectProperties.stream()
							.map( f -> f.formatted( writer.scopeTypeName ) )
							.collect( Collectors.joining( "\n\n" ) )
							.replaceAll( "(?m)^", "\t" )
			);
		}

		private String getFieldReferences(String scopeType) {
			if ( writer.regularProperties.isEmpty() && writer.objectProperties.isEmpty() ) {
				return "";
			}
			return Stream.concat(
					writer.regularProperties.stream().map( p -> p.asProperty( scopeType ) ),
					writer.objectProperties.stream().map( p -> p.asProperty( scopeType ) )
			)
					.collect( Collectors.joining( ";\n\tpublic final ", "public final ", ";\n" ) );
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
