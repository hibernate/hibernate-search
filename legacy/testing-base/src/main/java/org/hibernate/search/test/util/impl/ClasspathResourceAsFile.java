/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;


/**
 * Provides access to a file that is a copy of a given classpath resource.
 *
 * <p>Useful to test code that expects actual files on the filesystem using
 * resources from a different jar (which are zipped and are not actual files).
 *
 * @author Yoann Rodiere
 */
public class ClasspathResourceAsFile extends ExternalResource {

	private File parentDirectory;

	private final URL url;

	private File file;
	private boolean hasCreatedTempFile;

	public ClasspathResourceAsFile(Class<?> clazz, String path) {
		this( clazz, path, null );
	}

	public ClasspathResourceAsFile(Class<?> clazz, String path, File parentDirectory) {
		this.url = clazz.getResource( path );
		this.parentDirectory = parentDirectory;
	}

	public File get() {
		return file;
	}

	@Override
	protected void before() throws Throwable {
		createFileIfNecessary();
	}

	@Override
	protected void after() {
		deleteFileIfNecessary();
	}

	private void createFileIfNecessary() throws IOException {
		this.file = FileUtils.toFile( url );
		if ( file == null ) {
			this.file = File.createTempFile( "classPathResourceAsFile", getOriginalExtension(), parentDirectory );
			this.hasCreatedTempFile = true;
			try ( InputStream input = url.openStream(); OutputStream output = new FileOutputStream( file ) ) {
				IOUtils.copy( input, output );
			}
		}
	}

	private void deleteFileIfNecessary() {
		try {
			if ( hasCreatedTempFile ) {
				FileUtils.deleteQuietly( file );
			}
		}
		finally {
			this.file = null;
			this.hasCreatedTempFile = false;
		}
	}

	private String getOriginalExtension() {
		String extension = FilenameUtils.getExtension( url.getFile() );
		if ( extension == null || extension.isEmpty() ) {
			extension = null;
		}
		else {
			extension = "." + extension;
		}
		return extension;
	}

}
