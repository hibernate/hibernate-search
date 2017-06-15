/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Utility class for file and directory operations, like synchronisation and reading from class path.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FileHelper {

	private static final Log log = LoggerFactory.make();
	private static final int FAT_PRECISION = 2000;

	private FileHelper() {
	}

	public static boolean areInSync(Path source, Path destination) throws IOException {
		if ( Files.isDirectory( source ) ) {
			if ( ! Files.exists( destination ) ) {
				return false;
			}
			else if ( ! Files.isDirectory( destination ) ) {
				throw new IOException(
						"Source and Destination not of the same type:"
								+ source.toAbsolutePath().toString() + " , " + destination.toAbsolutePath().toString()
				);
			}
			final Set<Path> sources = listFiles( source );
			Set<String> sourcesFilenameSet = sources.stream().map( v -> v.getFileName().toString() ).collect( Collectors.toSet() );
			final Set<String> destinationFilenameSet;
			try ( Stream<Path> dests = Files.list( destination ) ) {
				destinationFilenameSet = dests.map( v -> v.getFileName().toString() ).collect( Collectors.toSet() );
			}

			// check for any file name mismatches first
			if ( ! sourcesFilenameSet.equals( destinationFilenameSet ) ) {
				return false;
			}

			boolean inSync = true;
			for ( Path src : sources ) {
				Path destFile = destination.resolve( src.getFileName() );
				if ( !areInSync( src, destFile ) ) {
					inSync = false;
					break;
				}
			}
			return inSync;
		}
		else {
			if ( Files.exists( destination ) && Files.isRegularFile( destination ) ) {
				//TODO see if with NIO there's a better way to compare timestamps
				long sts = Files.getLastModifiedTime( source ).toMillis() / FAT_PRECISION;
				long dts = Files.getLastModifiedTime( destination ).toMillis() / FAT_PRECISION;
				return sts == dts;
			}
			else {
				return false;
			}
		}
	}

	public static void synchronize(Path source, Path destination, boolean smart) throws IOException {
		if ( Files.isDirectory( source ) ) {
			Files.createDirectories( destination );
			final Set<Path> sources = listFiles( source );
			Set<String> sourceFilenames = sources.stream().map( v -> v.getFileName().toString() ).collect( Collectors.toSet() );
			final Set<Path> dests = listFiles( destination );
			Set<String> destFilenames = dests.stream().map( v -> v.getFileName().toString() ).collect( Collectors.toSet() );

			//delete files not present in source
			for ( String fileName : destFilenames ) {
				if ( !sourceFilenames.contains( fileName ) ) {
					delete( destination.resolve( fileName ) );
				}
			}
			//copy each file from source
			for ( Path srcFile : sources ) {
				Path destFile = destination.resolve( srcFile.getFileName() );
				synchronize( srcFile, destFile, smart );
			}
		}
		else {
			if ( Files.exists( destination ) && Files.isDirectory( destination ) ) {
				tryDelete( destination );
			}
			if ( Files.exists( destination ) ) {
				long sts = Files.getLastModifiedTime( source ).toMillis() / FAT_PRECISION;
				long dts = Files.getLastModifiedTime( destination ).toMillis() / FAT_PRECISION;
				//do not copy if smart and same timestamp and same length
				if ( !smart || sts == 0 || sts != dts || source.toFile().length() != destination.toFile().length() ) {
					copyFile( source, destination );
				}
			}
			else {
				copyFile( source, destination );
			}
		}
	}

	/**
	 * Lists all files in a directory, making sure the underlying stream is closed.
	 * @param directory the path to list files from
	 * @return a set of all contained paths
	 * @throws IOException
	 */
	private static Set<Path> listFiles(Path directory) throws IOException {
		try ( Stream<Path> stream = Files.list( directory ) ) {
			return stream.collect( Collectors.toSet() );
		}
	}

	private static void copyFile(Path source, Path destination) throws IOException {
		// Copy the attributes as well as we like the "modified" timestamp to be maintained
		Files.copy( source, destination, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING );
	}

	/**
	 * Deletes a file. If the file is a directory delete recursively all content.
	 *
	 * @param path the file or directory to be deleted
	 *
	 * @throws IOException if it wasn't possible to delete all content.
	 */
	public static void delete(Path path) throws IOException {
		deleteRecursive( path, false );
	}

	/**
	 * Attempts to delete a file. If the file is a directory delete recursively all content.
	 * Any IOException preventing a file to be deleted will be swallowed.
	 *
	 * @param path the file or directory to be deleted
	 *
	 * @throws IOException on unexpected io errors
	 */
	public static void tryDelete(Path path) throws IOException {
		deleteRecursive( path, true );
	}

	private static void deleteRecursive(Path path, boolean ignoreExceptions) throws IOException {
		if ( path == null ) {
			throw new IllegalArgumentException();
		}

		if ( Files.notExists( path ) ) {
			return;
		}

		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				delete( file, ignoreExceptions );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				delete( dir, ignoreExceptions );
				return FileVisitResult.CONTINUE;
			}
		} );
	}

	private static void delete(Path file, boolean ignoreExceptions) throws IOException {
		if ( ignoreExceptions ) {
			safeDelete( file );
		}
		else {
			deleteOrFail( file );
		}
	}

	private static void safeDelete(Path file) {
		try {
			Files.deleteIfExists( file );
		}
		catch (IOException e) {
			log.fileDeleteFailureIgnored( e );
		}
	}

	private static void deleteOrFail(Path file) throws IOException {
		Files.delete( file );
	}

}
