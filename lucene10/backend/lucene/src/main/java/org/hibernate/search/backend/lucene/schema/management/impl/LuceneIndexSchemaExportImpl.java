/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.schema.management.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.backend.lucene.schema.management.LuceneIndexSchemaExport;

public class LuceneIndexSchemaExportImpl implements LuceneIndexSchemaExport {

	private static final byte[] EXPORT_MESSAGE = "The Lucene backend does not support exporting the schema."
			.getBytes( StandardCharsets.UTF_8 );
	private final String indexName;

	public LuceneIndexSchemaExportImpl(String indexName) {
		this.indexName = indexName;
	}

	public String indexName() {
		return indexName;
	}

	@Override
	public void toFiles(Path targetDirectory) {
		try ( OutputStream outputStream = Files.newOutputStream(
				Files.createDirectories( targetDirectory ).resolve( "no-schema.txt" ) ) ) {
			outputStream.write( EXPORT_MESSAGE );
		}
		catch (IOException e) {
			throw LuceneMiscLog.INSTANCE.unableToExportSchema( indexName, e.getMessage(), e );
		}
	}
}
