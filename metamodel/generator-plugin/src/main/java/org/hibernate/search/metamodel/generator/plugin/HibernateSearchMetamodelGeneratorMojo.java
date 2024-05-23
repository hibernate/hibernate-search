/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metamodel.generator.plugin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

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

	@Parameter(property = "properties")
	Properties properties;

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
			project.addCompileSourceRoot( generatedMetamodelLocation.toString() );


			try ( SessionFactory sessionFactory = setupContext.setup( annotatedTypes() ) ) {
				SearchMapping mapping = Search.mapping( sessionFactory );

				Collection<? extends SearchIndexedEntity<?>> indexedEntities = mapping.allIndexedEntities();

				for ( SearchIndexedEntity<?> indexedEntity : indexedEntities ) {
					createClass( indexedEntity, generatedMetamodelLocation );
				}

				getLog().info( "Indexed entities: " + indexedEntities );

			}

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

	private boolean hasOrmMapper(List<Dependency> dependencies) {
		for ( Dependency dependency : dependencies ) {
			if ( "hibernate-search-mapper-orm".equals( dependency.getArtifactId() ) ) {
				return true;
			}
		}
		return false;
	}
}
