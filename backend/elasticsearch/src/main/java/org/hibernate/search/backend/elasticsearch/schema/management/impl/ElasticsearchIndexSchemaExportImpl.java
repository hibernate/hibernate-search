/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaExport;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class ElasticsearchIndexSchemaExportImpl implements ElasticsearchIndexSchemaExport {

	private final Gson userFacingGson;
	private final String indexName;
	private final ElasticsearchRequest request;

	public ElasticsearchIndexSchemaExportImpl(Gson userFacingGson, String indexName,
			ElasticsearchRequest request) {
		this.userFacingGson = userFacingGson;
		this.indexName = indexName;
		this.request = request;
	}

	@Override
	public void toFiles(Path targetDirectory) {
		JsonObject queryParams = new JsonObject();
		for ( Map.Entry<String, String> entry : request.parameters().entrySet() ) {
			queryParams.addProperty( entry.getKey(), entry.getValue() );
		}
		List<JsonObject> parts = request.bodyParts();
		JsonElement body = parts.size() == 1
				? parts.get( 0 )
				: parts.stream().collect( JsonArray::new, JsonArray::add, JsonArray::addAll );

		write( targetDirectory, "create-index.json", body );
		write( targetDirectory, "create-index-query-params.json", queryParams );
	}

	private void write(Path path, String filename, JsonElement data) {
		try ( OutputStream outputStream = Files.newOutputStream( Files.createDirectories( path ).resolve( filename ) );
				OutputStreamWriter writer = new OutputStreamWriter( outputStream, StandardCharsets.UTF_8 );
				JsonWriter jsonWriter = new JsonWriter( writer ) ) {
			userFacingGson.toJson( data, jsonWriter );
			jsonWriter.flush();
		}
		catch (IOException e) {
			throw ElasticsearchMiscLog.INSTANCE.unableToExportSchema( indexName, e.getMessage(), e );
		}
	}

	@Override
	public Map<String, String> parameters() {
		return Collections.unmodifiableMap( request.parameters() );
	}

	@Override
	public List<JsonObject> bodyParts() {
		return Collections.unmodifiableList( request.bodyParts() );
	}

}
