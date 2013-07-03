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
package org.hibernate.search.util.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
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
public abstract class FileHelper {

	private static final Log log = LoggerFactory.make();
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

	/**
	 * Attempts to delete a file. If the file is a directory delete recursive all content.
	 *
	 * @param file the file or Directory to be deleted
	 * @return false if it wasn't possible to delete it or all of the contents. A common problem on Windows systems.
	 */
	public static boolean delete(File file) {
		boolean allok = true;
		if ( file.isDirectory() ) {
			for ( File subFile : file.listFiles() ) {
				boolean deleted = delete( subFile );
				allok = allok && deleted;
			}
		}
		if ( allok && file.exists() ) {
			if ( !file.delete() ) {
				log.notDeleted( file );
				return false;
			}
		}
		return allok;
	}

	/**
	 * Reads the provided input stream into a string
	 *
	 * @param inputStream the input stream to read from
	 * @return the content of the input stream as string
	 * @throws java.io.IOException in case an error occurs reading from the input stream
	 */
	public static String readInputStream(InputStream inputStream) throws IOException {
		Writer writer = new StringWriter();
		try {
			char[] buffer = new char[1000];
			Reader reader = new BufferedReader( new InputStreamReader( inputStream, "UTF-8" ) );
			int r = reader.read( buffer );
			while ( r != -1 ) {
				writer.write( buffer, 0, r );
				r = reader.read( buffer );
			}
			return writer.toString();
		}
		finally {
			closeResource( writer );
		}
	}

	/**
	 * Load a resource from a specific classLoader
	 *
	 * @param resourceName the name of the resource
	 * @param classLoader the classloader to use, or null to try the ContextClassloader first or the loading one second.
	 * @return the resource contents as a String
	 */
	public static String readResourceAsString(String resourceName, ClassLoader classLoader) {
		InputStream in;
		if ( classLoader != null ) {
			in = classLoader.getResourceAsStream( resourceName );
		}
		else {
			in = openResource( resourceName );
		}
		if ( in == null ) {
			throw log.unableToLoadResource( resourceName );
		}
		String s;
		try {
			s = FileHelper.readInputStream( in );
		}
		catch (IOException e) {
			throw log.unableToReadFile( resourceName, e );
		}
		finally {
			closeResource( in );
		}
		return s;
	}

	public static InputStream openResource(String resourceName) {
		//try loading from application context first:
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream resource = classLoader.getResourceAsStream( resourceName );
		if ( resource != null ) {
			return resource;
		}
		else {
			classLoader = FileHelper.class.getClassLoader();
			return classLoader.getResourceAsStream( resourceName );
		}
	}

	/**
	 * Closes a resource without throwing IOExceptions
	 *
	 * @param resource the resource to close
	 */
	public static void closeResource(Closeable resource) {
		if ( resource != null ) {
			try {
				resource.close();
			}
			catch (IOException e) {
				//we don't really care if we can't close
				log.couldNotCloseResource( e );
			}
		}
	}
}
