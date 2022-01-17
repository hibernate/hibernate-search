/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JandexIndexingUtils.indexJarOrDirectory;
import static org.hibernate.search.util.impl.test.jar.JandexUtils.extractDeclaringClass;
import static org.hibernate.search.util.impl.test.jar.JandexUtils.findRuntimeAnnotations;
import static org.hibernate.search.util.impl.test.jar.JarUtils.determineJarOrDirectoryLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.util.impl.test.jar.JandexUtils;
import org.hibernate.search.util.impl.test.logging.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

public class GsonClassesTest {

	private static Index gsonIndex;
	private static Index backendElasticsearchIndex;

	@BeforeClass
	public static void index() throws IOException {
		gsonIndex = indexJarOrDirectory( determineJarOrDirectoryLocation( Gson.class, "Gson" ) );
		backendElasticsearchIndex = indexJarOrDirectory( determineJarOrDirectoryLocation(
				ElasticsearchExtension.class, "hibernate-search-backend-elasticsearch" ) );
	}

	@Test
	public void testNoMissingGsonAnnotatedClass() {
		Set<DotName> gsonAnnotations = findRuntimeAnnotations( gsonIndex );

		Set<DotName> annotatedClasses = new HashSet<>();
		for ( DotName gsonAnnotation : gsonAnnotations ) {
			for ( AnnotationInstance annotationInstance : backendElasticsearchIndex.getAnnotations( gsonAnnotation ) ) {
				DotName className = extractDeclaringClass( annotationInstance.target() ).name();
				annotatedClasses.add( className );
			}
		}

		Set<String> annotatedClassesAndSubclasses = JandexUtils.toStrings(
				JandexUtils.collectClassHierarchiesRecursively( backendElasticsearchIndex, annotatedClasses ) );

		Log.INSTANCE.infof( "GSON-annotated classes and their class hierarchy: %s", annotatedClassesAndSubclasses );
		assertThat( annotatedClassesAndSubclasses ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( annotatedClassesAndSubclasses );
	}

	@Test
	public void testNoMissingGsonContractImplementations() {
		List<DotName> gsonContracts = Arrays.asList(
				DotName.createSimple( TypeAdapterFactory.class.getName() ),
				DotName.createSimple( TypeAdapter.class.getName() )
		);

		Set<DotName> classes = new HashSet<>();
		for ( DotName gsonContract : gsonContracts ) {
			for ( ClassInfo implementor : backendElasticsearchIndex.getAllKnownSubclasses( gsonContract ) ) {
				classes.add( implementor.name() );
			}
		}

		Set<String> classesAndSubclasses = JandexUtils.toStrings(
				JandexUtils.collectClassHierarchiesRecursively( backendElasticsearchIndex, classes ) );

		Log.INSTANCE.infof( "Gson contract implementations and their class hierarchy: %s", classesAndSubclasses );
		assertThat( classesAndSubclasses ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( classesAndSubclasses );
	}

}