/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	public static final long DEFAULT_COPY_BUFFER_SIZE = 16 * 1024 * 1024; // 16 MB

	private FileHelper() {
	}

	public static boolean areInSync(File source, File destination) throws IOException {
		if ( source.isDirectory() ) {
			if ( !destination.exists() ) {
				return false;
			}
			else if ( !destination.isDirectory() ) {
				throw new IOException(
						"Source and Destination not of the same type:"
								+ source.getCanonicalPath() + " , " + destination.getCanonicalPath()
				);
			}
			String[] sources = source.list();
			Set<String> srcNames = new HashSet<String>( Arrays.asList( sources ) );
			String[] dests = destination.list();

			// check for files in destination and not in source
			for ( String fileName : dests ) {
				if ( !srcNames.contains( fileName ) ) {
					return false;
				}
			}

			boolean inSync = true;
			for ( String fileName : sources ) {
				File srcFile = new File( source, fileName );
				File destFile = new File( destination, fileName );
				if ( !areInSync( srcFile, destFile ) ) {
					inSync = false;
					break;
				}
			}
			return inSync;
		}
		else {
			if ( destination.exists() && destination.isFile() ) {
				long sts = source.lastModified() / FAT_PRECISION;
				long dts = destination.lastModified() / FAT_PRECISION;
				return sts == dts;
			}
			else {
				return false;
			}
		}
	}

	public static void synchronize(File source, File destination, boolean smart) throws IOException {
		synchronize( source, destination, smart, DEFAULT_COPY_BUFFER_SIZE );
	}

	public static void synchronize(File source, File destination, boolean smart, long chunkSize) throws IOException {
		if ( chunkSize <= 0 ) {
			log.checkSizeMustBePositive();
			chunkSize = DEFAULT_COPY_BUFFER_SIZE;
		}
		if ( source.isDirectory() ) {
			if ( !destination.exists() ) {
				if ( !destination.mkdirs() ) {
					throw new IOException( "Could not create path " + destination );
				}
			}
			else if ( !destination.isDirectory() ) {
				throw new IOException(
						"Source and Destination not of the same type:"
								+ source.getCanonicalPath() + " , " + destination.getCanonicalPath()
				);
			}
			String[] sources = source.list();
			Set<String> srcNames = new HashSet<String>( Arrays.asList( sources ) );
			String[] dests = destination.list();

			//delete files not present in source
			for ( String fileName : dests ) {
				if ( !srcNames.contains( fileName ) ) {
					delete( new File( destination, fileName ) );
				}
			}
			//copy each file from source
			for ( String fileName : sources ) {
				File srcFile = new File( source, fileName );
				File destFile = new File( destination, fileName );
				synchronize( srcFile, destFile, smart, chunkSize );
			}
		}
		else {
			if ( destination.exists() && destination.isDirectory() ) {
				delete( destination );
			}
			if ( destination.exists() ) {
				long sts = source.lastModified() / FAT_PRECISION;
				long dts = destination.lastModified() / FAT_PRECISION;
				//do not copy if smart and same timestamp and same length
				if ( !smart || sts == 0 || sts != dts || source.length() != destination.length() ) {
					copyFile( source, destination, chunkSize );
				}
			}
			else {
				copyFile( source, destination, chunkSize );
			}
		}
	}

	private static void copyFile(File srcFile, File destFile, long chunkSize) throws IOException {
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream( srcFile );
			FileChannel iChannel = is.getChannel();
			os = new FileOutputStream( destFile, false );
			FileChannel oChannel = os.getChannel();
			long doneBytes = 0L;
			long todoBytes = srcFile.length();
			while ( todoBytes != 0L ) {
				long iterationBytes = Math.min( todoBytes, chunkSize );
				long transferredLength = oChannel.transferFrom( iChannel, doneBytes, iterationBytes );
				if ( iterationBytes != transferredLength ) {
					throw new IOException(
							"Error during file transfer: expected "
									+ iterationBytes + " bytes, only " + transferredLength + " bytes copied."
					);
				}
				doneBytes += transferredLength;
				todoBytes -= transferredLength;
			}
		}
		finally {
			if ( is != null ) {
				is.close();
			}
			if ( os != null ) {
				os.close();
			}
		}
		boolean successTimestampOp = destFile.setLastModified( srcFile.lastModified() );
		if ( !successTimestampOp ) {
			log.notChangeTimestamp( destFile );
		}
	}

	@Deprecated
	public static void delete(File file) throws IOException {
		delete( file.toPath() );
	}

	/**
	 * Attempts to delete a file. If the file is a directory delete recursively all content.
	 *
	 * @param path the file or directory to be deleted
	 *
	 * @throws IOException if it wasn't possible to delete all content which is a common problem on Windows systems.
	 */
	public static void delete(Path path) throws IOException {
		if ( path == null ) {
			throw new IllegalArgumentException();
		}

		if ( Files.notExists( path ) ) {
			return;
		}

		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete( file );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete( dir );
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
