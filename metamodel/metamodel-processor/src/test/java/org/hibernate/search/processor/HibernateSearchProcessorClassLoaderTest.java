package org.hibernate.search.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HibernateSearchProcessorClassLoaderTest {

	@TempDir
	Path output;

	@Test
	void isolatedApplicationContextClassLoader() throws Exception {
		List<File> dependencies = dependencies();
		URL[] urls = dependencies.stream().map( file -> {
			try {
				return file.toURI().toURL();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} ).toArray( URL[]::new );
		try ( URLClassLoader applicationLoader = new URLClassLoader( urls, ClassLoader.getPlatformClassLoader() ) ) {
			Class<?> applicationContract = applicationLoader.loadClass(
					"org.hibernate.search.engine.environment.bean.spi.BeanConfigurer" );
			assertThat( applicationContract ).isNotSameAs(
					org.hibernate.search.engine.environment.bean.spi.BeanConfigurer.class );
			assertThat( applicationContract.isAssignableFrom( applicationLoader.loadClass(
					"org.hibernate.search.mapper.orm.impl.HibernateOrmBeanConfigurer" ) ) ).isTrue();
			compile( applicationLoader );
		}
	}

	@Test
	void nullContextClassLoader() throws Exception {
		compile( null );
	}

	@Test
	void systemContextClassLoader() throws Exception {
		compile( ClassLoader.getSystemClassLoader() );
	}

	private void compile(ClassLoader contextLoader) throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader original = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader( contextLoader );
			var compiler = ToolProvider.getSystemJavaCompiler();
			var diagnostics = new DiagnosticCollector<JavaFileObject>();
			try ( var files = compiler.getStandardFileManager( diagnostics, null, null ) ) {
				List<File> processorPath = new ArrayList<>( dependencies().stream()
						.filter( file -> !file.getName().equals( "hibernate-search-mapper-orm.jar" ) )
						.toList() );
				processorPath.add( target().resolve( "classes" ).toFile() );
				files.setLocation( StandardLocation.ANNOTATION_PROCESSOR_PATH, processorPath );
				// The test JVM itself contains Search: prevent javac's processor loader from
				// delegating to that copy, just as an external compiler host would.
				URL[] processorUrls = new URL[processorPath.size()];
				for ( int i = 0; i < processorPath.size(); i++ ) {
					processorUrls[i] = processorPath.get( i ).toURI().toURL();
				}
				try ( URLClassLoader processorLoader =
						new URLClassLoader( processorUrls, ClassLoader.getPlatformClassLoader() ) ) {
					var isolatedFiles = new ForwardingJavaFileManager<>( files ) {
						@Override
						public ClassLoader getClassLoader(Location location) {
							return location == StandardLocation.ANNOTATION_PROCESSOR_PATH
									? processorLoader
									: super.getClassLoader( location );
						}
					};
					files.setLocation( StandardLocation.CLASS_PATH, dependencies() );
					files.setLocationFromPaths( StandardLocation.CLASS_OUTPUT, List.of( output ) );
					files.setLocationFromPaths( StandardLocation.SOURCE_OUTPUT, List.of( output ) );
					JavaFileObject source = new SimpleJavaFileObject( URI.create( "string:///example/Book.java" ),
							JavaFileObject.Kind.SOURCE ) {
						@Override
						public CharSequence getCharContent(boolean ignoreEncodingErrors) {
							return """
									package example;
									import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
									import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
									import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
									import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
									import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
									import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
									import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
									@Indexed
									public class Book {
										@DocumentId String id;
										@FullTextField String title;
										@KeywordField(valueBinder = @ValueBinderRef(type = SourceOnlyBinder.class))
										String custom;
										@KeywordField(valueBridge = @ValueBridgeRef(type = SourceOnlyBridge.class))
										String bridged;
										public static class SourceOnlyBridge implements ValueBridge<String, String> {
											public String toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
												throw new IllegalStateException("Must never be instantiated");
											}
										}
										public static class SourceOnlyBinder implements ValueBinder {
											public void bind(ValueBindingContext context) {
											throw new IllegalStateException("Must be inspected through javac, never instantiated");
											}
										}
									}
									""";
						}
					};
					boolean success = compiler.getTask( null, isolatedFiles, diagnostics,
							List.of( "-processor", HibernateSearchProcessor.class.getName() ),
							null, List.of( source ) ).call();
					assertThat( success ).as( diagnostics.getDiagnostics().toString() ).isTrue();
					assertThat( Files.readString( output.resolve( "example/Book__.java" ) ) )
							.contains( "java.lang.String> title;", "\"title\"" )
							.doesNotContain( "> custom;", "> bridged;" );
					assertThat( diagnostics.getDiagnostics() )
							.anySatisfy( diagnostic -> assertThat( diagnostic.getMessage( null ) )
									.contains( "Defined value binder", "is ignored" ) );
				}
			}
		}
		finally {
			thread.setContextClassLoader( original );
		}
	}

	private static List<File> dependencies() throws Exception {
		try ( var files = Files.list( target().resolve( "test-dependencies" ) ) ) {
			return files.filter( path -> path.toString().endsWith( ".jar" ) ).map( Path::toFile ).toList();
		}
	}

	private static Path target() throws Exception {
		return Path.of( HibernateSearchProcessorClassLoaderTest.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI() ).getParent();
	}
}
