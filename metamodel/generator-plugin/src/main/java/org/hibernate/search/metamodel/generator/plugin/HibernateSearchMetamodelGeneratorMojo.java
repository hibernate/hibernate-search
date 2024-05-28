/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metamodel.generator.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-metamodel", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class HibernateSearchMetamodelGeneratorMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(property = "annotatedTypes")
	List<String> annotatedTypes;

	@Parameter(property = "packagesToCompile")
	List<String> packagesToCompile;

	@Parameter(property = "properties")
	Properties properties;

	@Parameter(property = "sameModuleCompile", defaultValue = "false")
	boolean sameModuleCompile;

	private URLClassLoader classLoader;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Hibernate Search Metamodel Generator" );
		getLog().info( "Dependencies: " + project.getDependencies() );

		if ( hasOrmMapper( project.getDependencies() ) ) {
			getLog().info( "Sources: " + project.getCompileSourceRoots() );
			OrmSetupHelper.SetupContext setupContext =
					OrmSetupHelper.withSingleBackend( new LuceneBackendConfiguration() ).start();

			properties.forEach( (k, v) -> setupContext.withProperty( Objects.toString( k ), v ) );

			Path generatedMetamodelLocation =
					Path.of( project.getBuild().getOutputDirectory() ).resolveSibling( "generated-metamodel-sources" );

			try ( SessionFactory sessionFactory = setupContext.setup( annotatedTypes() ) ) {
				SearchMapping mapping = Search.mapping( sessionFactory );

				Collection<? extends SearchIndexedEntity<?>> indexedEntities = mapping.allIndexedEntities();

				for ( SearchIndexedEntity<?> indexedEntity : indexedEntities ) {
					createClass( indexedEntity, generatedMetamodelLocation );
				}

				getLog().info( "Indexed entities: " + indexedEntities );

			}
			project.addCompileSourceRoot( generatedMetamodelLocation.toString() );
		}
	}

	private void createClass(SearchIndexedEntity<?> indexedEntity, Path root) {
		getLog().info( "Creating class for entity: " + indexedEntity.jpaName() );

		IndexDescriptor descriptor = indexedEntity.indexManager().descriptor();

		StringBuilder fields = new StringBuilder();

		for ( IndexFieldDescriptor staticField : descriptor.staticFields() ) {
			fields.append( '\n' )
					.append( '\t' ).append( "public String " ).append( staticField.relativeName() ).append( ";" );
		}

		try {
			Class<?> javaClass = indexedEntity.javaClass();
			Path pckg = root.resolve( Path.of( javaClass.getPackageName().replace( '.', '/' ) ) );
			Files.createDirectories( pckg );
			try ( FileOutputStream os =
					new FileOutputStream( pckg.resolve( javaClass.getSimpleName() + ".java" ).toFile() ); ) {
				os.write( new StringBuilder().append( "package " ).append( javaClass.getPackageName() ).append( ";\n\n" )
						.append( "class " ).append( javaClass.getSimpleName() ).append( "__ {\n" )
						.append( fields )
						.append( "\t\n}" )
						.toString().getBytes( StandardCharsets.UTF_8 ) );
			}

		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

	}

	private Class<?>[] annotatedTypes() {
		if ( sameModuleCompile ) {
			try {
				List<Path> roots = new ArrayList<>();
				List<Path> classes = new ArrayList<>();
				for ( String compileSourceRoot : this.project.getCompileSourceRoots() ) {
					Path root = Path.of( compileSourceRoot );
					roots.add( root );

					for ( String pkg : packagesToCompile ) {
						Path path = root.resolve( Path.of( pkg.replace( ".", FileSystems.getDefault().getSeparator() ) ) );
						if ( Files.exists( path ) ) {
							Files.list( path ).filter( f -> f.getFileName().toString().endsWith( ".java" ) )
									.forEach( classes::add );
						}
					}
				}

				Path output = Path.of( project.getBuild().getOutputDirectory() )
						.resolveSibling( "generated-metamodel-pre-compiled-classes" );
				Files.createDirectories( output );

				JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
				fileManager.setLocationFromPaths( StandardLocation.SOURCE_PATH, roots );
				fileManager.setLocation( StandardLocation.CLASS_PATH, getDependenciesAsFiles() );
				fileManager.setLocationFromPaths( StandardLocation.CLASS_OUTPUT, List.of( output ) );

				Iterable<? extends JavaFileObject> toCompile = fileManager.getJavaFileObjectsFromPaths( classes );

				DiagnosticCollector<JavaFileObject> diagnostic = new DiagnosticCollector<>();
				JavaCompiler.CompilationTask task =
						compiler.getTask( null, fileManager, diagnostic, List.of(), null, toCompile );

				task.call();

				classLoader = new URLClassLoader( "hibernate-search-generator",
						new URL[] { output.toUri().toURL() }, this.getClass().getClassLoader() );
				Thread.currentThread().setContextClassLoader( classLoader );

				Class<?>[] types = new Class<?>[annotatedTypes.size()];
				for ( int i = 0; i < annotatedTypes.size(); i++ ) {
					types[i] = classLoader.loadClass( annotatedTypes.get( i ) );
				}
				return types;

			}
			catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException( e );
			}
		}
		else {
			try {
				Class<?>[] types = new Class<?>[annotatedTypes.size()];
				for ( int i = 0; i < annotatedTypes.size(); i++ ) {
					types[i] = Class.forName( annotatedTypes.get( i ) );
				}
				return types;
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException( e );
			}
		}
	}

	private Collection<File> getDependenciesAsFiles() {
		project.setArtifactFilter( artifact -> true );
		project.setArtifacts( null );
		return project.getArtifacts()
				.stream()
				.map( Artifact::getFile )
				.collect( Collectors.toList() );
	}

	private boolean hasOrmMapper(List<Dependency> dependencies) {
		for ( Dependency dependency : dependencies ) {
			if ( "hibernate-search-mapper-orm".equals( dependency.getArtifactId() ) ) {
				return true;
			}
		}
		return false;
	}
}
