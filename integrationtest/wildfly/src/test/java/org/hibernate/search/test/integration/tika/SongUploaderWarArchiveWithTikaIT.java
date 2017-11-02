/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.tika;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.tika.controller.SongUploader;
import org.hibernate.search.test.integration.tika.model.Mp3TikaMetadataProcessor;
import org.hibernate.search.test.integration.tika.model.Song;
import org.hibernate.search.test.integration.tika.util.Resources;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * An integration test running on WildFly to test integration with an Apache Tika
 * module, which we don't provide but could be packaged by users.
 * The hibernate-search-engine module imports this optional module, we need to test
 * that whenever it's being provided it actually works.
 */
@RunWith(Arquillian.class)
public class SongUploaderWarArchiveWithTikaIT {

	public static final String CONFIGURATION_PROPERTIES_RESOURCENAME = "configuration.properties";
	public static final String PROPERTY_KEY = "mp3AbsolutePath";

	@Deployment
	public static Archive<?> createTestArchive() {
		String mp3AbsolutePath = SongUploaderWarArchiveWithTikaIT.class.getClassLoader()
				.getResource( "org/hibernate/search/test/bridge/tika/mysong.mp3" )
				.getFile(); //Possibly broken on Windows?
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, SongUploaderWarArchiveWithTikaIT.class.getSimpleName() + ".war" )
				.addClasses( SongUploaderWarArchiveWithTikaIT.class, Song.class, SongUploader.class, Resources.class, Mp3TikaMetadataProcessor.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsResource( new StringAsset( PROPERTY_KEY + "=" + mp3AbsolutePath ), CONFIGURATION_PROPERTIES_RESOURCENAME )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.add( tikaDependenciesManifest(), "META-INF/MANIFEST.MF" )
				;
		return war;
	}

	private static Asset tikaDependenciesManifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", "org.apache.tika:" + VersionTestHelper.getDependencyVersionTika() )
				.exportAsString();
		return new StringAsset( manifest );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	SongUploader songUploader;

	@Test
	public void testSongUpload() throws Exception {
		Song newSong = songUploader.getNewSong();
		newSong.setMp3FileName( readAbsolutePath() );
		songUploader.upload();
		assertNotNull( newSong.getId() );
		List<Song> resultsList = songUploader.search( "mysong" );
		assertEquals( 1, resultsList.size() );
	}

	/**
	 * We need to write the absolute Path as a string into the deployment,
	 * as the resource lookup won't work when this method is invoked from
	 * within the server: we don't include the MP3 file into the deployment.
	 */
	private String readAbsolutePath() throws IOException {
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final Properties p = new Properties();
		try ( InputStream inputStream = classLoader.getResourceAsStream( SongUploaderWarArchiveWithTikaIT.CONFIGURATION_PROPERTIES_RESOURCENAME ) ) {
			p.load( inputStream );
		}
		return p.getProperty( PROPERTY_KEY );
	}

}
