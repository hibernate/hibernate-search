/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.FileSystemAccessStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.LockFactory;

public class LocalFileSystemDirectoryProvider implements DirectoryProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME = "local-filesystem";

	private static final ConfigurationProperty<Path> ROOT =
			ConfigurationProperty.forKey( LuceneBackendSettings.DirectoryRadicals.ROOT )
					.as( Path.class, Paths::get )
					.withDefault( () -> Paths.get( LuceneBackendSettings.Defaults.DIRECTORY_ROOT ) )
					.build();

	private static final ConfigurationProperty<FileSystemAccessStrategyName> FILESYSTEM_ACCESS_STRATEGY =
			ConfigurationProperty.forKey( LuceneBackendSettings.DirectoryRadicals.FILESYSTEM_ACCESS_STRATEGY )
					.as( FileSystemAccessStrategyName.class, FileSystemAccessStrategyName::of )
					.withDefault( LuceneBackendSettings.Defaults.DIRECTORY_FILESYSTEM_ACCESS_STRATEGY )
					.build();

	private Path root;
	private FileSystemAccessStrategy accessStrategy;
	private LockFactory lockFactory;

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + "root=" + root + "]";
	}

	@Override
	public void initialize(DirectoryProviderInitializationContext context) {
		ConfigurationPropertySource propertySource = context.getConfigurationPropertySource();
		this.root = ROOT.get( propertySource ).toAbsolutePath();
		FileSystemAccessStrategyName accessStrategyName = FILESYSTEM_ACCESS_STRATEGY.get( propertySource );
		this.accessStrategy = FileSystemAccessStrategy.get( accessStrategyName );
		this.lockFactory = context.createConfiguredLockFactory().orElseGet( FSLockFactory::getDefault );

		try {
			initializeWriteableDirectory( root );
		}
		catch (Exception e) {
			throw log.unableToInitializeRootDirectory( root, e.getMessage(), e );
		}
	}

	@Override
	public DirectoryHolder createDirectory(DirectoryCreationContext context) throws IOException {
		Path directoryPath = root.resolve( context.getIndexName() );
		try {
			initializeWriteableDirectory( directoryPath );
		}
		catch (Exception e) {
			throw log.unableToInitializeIndexDirectory( e.getMessage(), context.getEventContext(), e );
		}
		Directory directory = accessStrategy.createDirectory( directoryPath, lockFactory );
		try {
			context.initializeIndexIfNeeded( directory );
			return DirectoryHolder.of( directory );
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
	}

	private void initializeWriteableDirectory(Path rootDirectory) throws IOException {
		if ( Files.exists( rootDirectory ) ) {
			if ( !Files.isDirectory( rootDirectory ) || !Files.isWritable( rootDirectory ) ) {
				throw log.pathIsNotWriteableDirectory( rootDirectory );
			}
		}
		else {
			Files.createDirectories( rootDirectory );
		}
	}

}
