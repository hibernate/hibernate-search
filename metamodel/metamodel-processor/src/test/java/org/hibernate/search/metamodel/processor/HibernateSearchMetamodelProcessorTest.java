/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.hibernate.search.metamodel.processor.model.FieldTypesEntity;
import org.hibernate.search.metamodel.processor.model.ISBN;
import org.hibernate.search.metamodel.processor.model.MyContainedEntity;
import org.hibernate.search.metamodel.processor.model.MyContainingEntity;
import org.hibernate.search.metamodel.processor.model.MyCustomField;
import org.hibernate.search.metamodel.processor.model.MyCustomId;
import org.hibernate.search.metamodel.processor.model.MyEmbeddedEntity;
import org.hibernate.search.metamodel.processor.model.MyEntityWithBinders;
import org.hibernate.search.metamodel.processor.model.MyEnum;
import org.hibernate.search.metamodel.processor.model.MyEnumCollectionEntity;
import org.hibernate.search.metamodel.processor.model.MyFieldBinderIndexedEntity;
import org.hibernate.search.metamodel.processor.model.MyFieldBridgeIndexedEntity;
import org.hibernate.search.metamodel.processor.model.MyGeoPointBindingFieldEntity;
import org.hibernate.search.metamodel.processor.model.MyIdBinderIndexedEntity;
import org.hibernate.search.metamodel.processor.model.MyIdBridgeIndexedEntity;
import org.hibernate.search.metamodel.processor.model.MyIndexedEntity;
import org.hibernate.search.metamodel.processor.model.MyIndexedGetterEntity;
import org.hibernate.search.metamodel.processor.model.SomeGenerics;
import org.hibernate.search.metamodel.processor.model.SomeRandomType;
import org.hibernate.search.metamodel.processor.model.SomeRandomTypeBinder;

import org.junit.jupiter.api.Test;

class HibernateSearchMetamodelProcessorTest {

	private static final Path BASE_DIR;
	private static final Path TARGET_DIR;
	private static final Path PROCESSOR_OUT_DIR;

	static {
		TARGET_DIR = getTargetDir();
		BASE_DIR = TARGET_DIR.getParent();
		PROCESSOR_OUT_DIR = TARGET_DIR.resolve( "processor-generated-test-classes" );
		if ( !Files.exists( PROCESSOR_OUT_DIR ) ) {
			try {
				Files.createDirectories( PROCESSOR_OUT_DIR );
			}
			catch (IOException e) {
				fail( "Unable to create test output directory " + PROCESSOR_OUT_DIR );
			}
		}
	}

	@Test
	void smoke() {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		compile(
				new HibernateSearchMetamodelProcessor(), diagnostics,
				getSourceFile( SomeGenerics.class ),
				getSourceFile( MyIndexedEntity.class ),
				getSourceFile( MyEmbeddedEntity.class ),
				getSourceFile( MyIndexedGetterEntity.class ),
				getSourceFile( SomeRandomType.class ),
				getSourceFile( SomeRandomTypeBinder.class ),
				getSourceFile( ISBN.class ),
				getSourceFile( MyEnum.class ),
				getSourceFile( FieldTypesEntity.class ),

				getSourceFile( MyCustomId.class ),
				getSourceFile( MyIdBinderIndexedEntity.class ),
				getSourceFile( MyIdBridgeIndexedEntity.class ),

				getSourceFile( MyCustomField.class ),
				getSourceFile( MyFieldBinderIndexedEntity.class ),
				getSourceFile( MyFieldBridgeIndexedEntity.class ),

				getSourceFile( MyContainedEntity.class ),
				getSourceFile( MyContainingEntity.class ),

				getSourceFile( MyEnumCollectionEntity.class ),

				getSourceFile( MyGeoPointBindingFieldEntity.class )
		);
		diagnostics.getDiagnostics().forEach( System.out::println );

		assertThat( diagnostics.getDiagnostics().stream().map( Diagnostic::getKind ) )
				.doesNotContain( Diagnostic.Kind.ERROR );
	}

	@Test
	void binderWarning() {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		compile(
				new HibernateSearchMetamodelProcessor(), diagnostics,
				getSourceFile( MyEntityWithBinders.class ),
				getSourceFile( SomeRandomType.class ),
				getSourceFile( SomeRandomTypeBinder.class )
		);
		diagnostics.getDiagnostics().forEach( System.out::println );

		assertThat( diagnostics.getDiagnostics().stream().map( Diagnostic::getKind ) )
				.contains( Diagnostic.Kind.WARNING );
	}

	public boolean compile(Processor annotationProcessor, DiagnosticCollector<JavaFileObject> diagnostics,
			File... sourceFiles) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects( sourceFiles );

		try {
			fileManager.setLocation( StandardLocation.CLASS_PATH, dependencies() );
			fileManager.setLocation( StandardLocation.CLASS_OUTPUT, List.of( PROCESSOR_OUT_DIR.toFile() ) );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		List<String> options = List.of();

		JavaCompiler.CompilationTask task = compiler.getTask( null, fileManager, diagnostics, options, null, compilationUnits );
		task.setProcessors( List.of( annotationProcessor ) );

		return task.call();
	}

	private Iterable<? extends File> dependencies() {
		return Set.of(
				dependency( "hibernate-search-mapper-pojo-base.jar" ),
				dependency( "hibernate-search-mapper-pojo-standalone.jar" ),
				dependency( "hibernate-search-engine.jar" )
		);
	}

	private File dependency(String name) {
		return TARGET_DIR.toAbsolutePath().resolve( "test-dependencies" ).resolve( name ).toFile();
	}

	public File getSourceFile(Class<?> clazz) {
		String sourceFileName = clazz.getName().replace( ".", File.separator ) + ".java";
		return BASE_DIR.toAbsolutePath().resolve( "src" ).resolve( "test" ).resolve( "java" ).resolve( sourceFileName )
				.toFile();
	}


	private static Path getTargetDir() {
		try {
			// target/test-classes
			var targetClassesDir = HibernateSearchMetamodelProcessorTest.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI();
			// use URI to make things work on Win as well:
			return Paths.get( targetClassesDir ).getParent();
		}
		catch (URISyntaxException e) {
			fail( e.getMessage() );
			return null;
		}
	}

}
