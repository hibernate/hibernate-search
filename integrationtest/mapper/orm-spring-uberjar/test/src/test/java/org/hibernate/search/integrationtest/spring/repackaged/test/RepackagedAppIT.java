/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.repackaged.test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarEntry;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.util.common.jar.impl.JandexUtils;

import org.junit.Test;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;

import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyEntity;
import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyProjection;
import org.springframework.boot.loader.jar.JarFile;

public class RepackagedAppIT {

	/**
	 * Test makes sure that our utils can read the classes from the nested jar inside a Spring's repackaged uberjar.
	 * We try to create index and then see that that index has the info we need e.g. projection/entity classes etc.
	 */
	@Test
	public void canReadJar() throws Exception {
		Path target = Path.of( this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI() );
		Path jar = target.resolve( "../lib/app.jar" );

		try ( JarFile outerJar = new JarFile( jar.toFile() ) ) {
			for ( JarEntry jarEntry : outerJar ) {
				if ( jarEntry.getName().contains( "hibernate-search-integrationtest-spring-repackaged-model" ) ) {
					URL innerJarURL = outerJar.getNestedJarFile( jarEntry ).getUrl();

					try ( URLClassLoader isolatedClassLoader = new URLClassLoader( new URL[] { innerJarURL }, null ) ) {
						Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( MyEntity.class.getName() );
						URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
						assertThat( location.getProtocol() ).isEqualTo( "jar" );
						Index index = JandexUtils.readOrBuildIndex( location );

						ClassInfo classInfo = index.getClassByName( DotName.createSimple( MyEntity.class.getName() ) );
						assertThat( classInfo ).isNotNull();
						FieldInfo name = classInfo.field( "name" );
						assertThat( name ).isNotNull();
						assertThat( name.type().name().toString() ).isEqualTo( String.class.getName() );

						classInfo = index.getClassByName( DotName.createSimple( MyProjection.class.getName() ) );
						assertThat( classInfo ).isNotNull();
						assertThat(
								classInfo.annotation( DotName.createSimple( ProjectionConstructor.class.getName() ) ) )
								.isNotNull();
						name = classInfo.field( "name" );
						assertThat( name ).isNotNull();
						assertThat( name.type().name().toString() ).isEqualTo( String.class.getName() );
					}
				}
			}
		}
	}
}
