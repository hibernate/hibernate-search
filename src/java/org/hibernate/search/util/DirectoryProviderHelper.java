//$Id$
package org.hibernate.search.util;

import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.annotations.common.util.StringHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Emmanuel Bernard
 */
public class DirectoryProviderHelper {
	private static Log log = LogFactory.getLog( DirectoryProviderHelper.class );
	/**
	 * Build a directory name out of a root and relative path, guessing the significant part
	 * and checking for the file availability
	 * 
	 */
	public static String getSourceDirectory(String rootPropertyName, String relativePropertyName,
											String directoryProviderName, Properties properties) {
		//TODO check that it's a directory
		String root = properties.getProperty( rootPropertyName );
		String relative = properties.getProperty( relativePropertyName );
		if ( log.isTraceEnabled() ) {
			log.trace(
					"Guess source directory from " + rootPropertyName + " " + root != null ? root : "<null>"
							+ " and " + relativePropertyName + " " + relative != null ? relative : "<null>"
			);
		}
		if (relative == null) relative = directoryProviderName;
		if ( StringHelper.isEmpty( root ) ) {
			log.debug( "No root directory, go with relative " + relative );
			File sourceFile = new File(relative);
			if ( ! sourceFile.exists() ) {
				throw new SearchException("Unable to read source directory: " + relative);
			}
			//else keep source as it
		}
		else {
			File rootDir = new File(root);
			if ( ! rootDir.exists() ) {
				rootDir.mkdirs();
			}
			else if ( ! rootDir.isDirectory() ) {
				throw new SearchException(rootPropertyName + " is not a directory");
			}
			//test it again in case mkdir failed for wrong reasons
			if ( rootDir.exists() ) {
				File sourceFile = new File(root, relative);
				if (! sourceFile.exists() ) sourceFile.mkdirs();
				log.debug( "Get directory from root + relative");
				try {
					relative = sourceFile.getCanonicalPath();
				}
				catch (IOException e) {
					throw new AssertionFailure("Unable to get canonical path: " + root + " + " + relative);
				}
			}
			else {
				throw new SearchException(rootPropertyName + " does not exist and cannot be created");
			}
		}
		return relative;
	}

	public static File determineIndexDir(String directoryProviderName, Properties properties) {
		String indexBase = properties.getProperty( "indexBase", "." );
		String indexName = properties.getProperty( "indexName", directoryProviderName );
		File indexDir = new File( indexBase );
		if ( ! indexDir.exists() ) {
			//if the base directory does not exist, create it
			//we do not fear concurrent creation since mkdir does not raise exceptions
			indexDir.mkdirs();
		}
		else if ( ! indexDir.isDirectory() ) {
			throw new SearchException( MessageFormat.format( "Index directory is not a directory: {0}", indexBase ) );
		}
		if ( !indexDir.canWrite() ) {
			throw new SearchException( "Cannot write into index directory: "
					+ ( indexDir.isAbsolute() ? indexBase : indexDir.getAbsolutePath() ) );
		}

		indexDir = new File( indexDir, indexName );
		return indexDir;
	}
}
