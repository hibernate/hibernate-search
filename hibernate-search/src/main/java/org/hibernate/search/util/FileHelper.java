/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

/**
 * Utility class for synchronizing files/directories.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public abstract class FileHelper {

	private static final Logger log = LoggerFactory.make();
	private static final int FAT_PRECISION = 2000;
	public static final long DEFAULT_COPY_BUFFER_SIZE = 16 * 1024 * 1024; // 16 MB


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
			log.warn( "Chunk size must be positive: using default value." );
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
			log.warn( "Could not change timestamp for {}. Index synchronization may be slow.", destFile );
		}
	}

	public static void delete(File file) {
		if ( file.isDirectory() ) {
			for ( File subFile : file.listFiles() ) {
				delete( subFile );
			}
		}
		if ( file.exists() ) {
			if ( !file.delete() ) {
				log.error( "Could not delete {}", file );
			}
		}
	}
}
