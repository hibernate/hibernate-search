//$Id$
package org.hibernate.search.util;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.nio.channels.FileChannel;

/**
 * @author Emmanuel Bernard
 */
public abstract class FileHelper {
	private static final int FAT_PRECISION = 2000;

	public static void synchronize(File source, File destination, boolean smart) throws IOException {
		if ( source.isDirectory() ) {
			if ( ! destination.exists() ) {
				destination.mkdirs();
			}
			else if ( ! destination.isDirectory() ) {
				throw new IOException("Source and Destination not of the same type:"
						+ source.getCanonicalPath() + " , " + destination.getCanonicalPath() );
			}
			String[] sources = source.list();
			Set<String> srcNames = new HashSet<String>( Arrays.asList( sources ) );
			String[] dests = destination.list();

			//delete files not present in source
			for (String fileName : dests) {
				if ( ! srcNames.contains( fileName ) ) {
					delete( new File(destination, fileName) );
				}
			}
			//copy each file from source
			for (String fileName : sources) {
				File srcFile = new File(source, fileName);
				File destFile = new File(destination, fileName);
				synchronize( srcFile, destFile, smart );
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
					copyFile(source, destination);
				}
			}
			else {
				copyFile(source, destination);
			}
		}
	}

	private static void copyFile(File srcFile, File destFile) throws IOException {
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(srcFile);
			FileChannel iChannel = is.getChannel();
			os = new FileOutputStream( destFile, false );
			FileChannel oChannel = os.getChannel();
			oChannel.transferFrom( iChannel, 0, srcFile.length() );
		}
		finally {
			if (is != null) is.close();
			if (os != null) os.close();
		}
		destFile.setLastModified( srcFile.lastModified() );
	}

	public static void delete(File file) {
		if ( file.isDirectory() ) {
			for ( File subFile : file.listFiles() ) delete( subFile );
		}
		if ( file.exists() ) file.delete();
	}
}
