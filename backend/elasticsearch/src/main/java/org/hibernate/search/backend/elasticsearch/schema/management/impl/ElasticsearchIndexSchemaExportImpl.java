package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaExport;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchIndexSchemaExportImpl implements ElasticsearchIndexSchemaExport {

	private final JsonCodecService userFacingCodec;
	private final String indexName;
	private final ElasticsearchJsonRequest request;

	public ElasticsearchIndexSchemaExportImpl(JsonCodecService userFacingCodec, String indexName,
			ElasticsearchJsonRequest request) {
		this.userFacingCodec = userFacingCodec;
		this.indexName = indexName;
		this.request = request;
	}

	@Override
	public void toFiles(Path targetDirectory) {
		ObjectNode queryParams = new ObjectNode( JsonNodeFactory.instance );
		for ( Map.Entry<String, String> entry : request.parameters().entrySet() ) {
			queryParams.put( entry.getKey(), entry.getValue() );
		}
		List<ObjectNode> parts = request.bodyPartsJson();
		JsonNode body = parts.size() == 1
				? parts.get( 0 )
				: parts.stream().collect( () -> new ArrayNode( JsonNodeFactory.instance ), ArrayNode::add, ArrayNode::addAll );

		write( targetDirectory, "create-index.json", body );
		write( targetDirectory, "create-index-query-params.json", queryParams );
	}

	private void write(Path path, String filename, JsonNode data) {
		try ( OutputStream outputStream = Files.newOutputStream( Files.createDirectories( path ).resolve( filename ) ) ) {
			userFacingCodec.writeCompact( data, outputStream );
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
	@Deprecated
	public List<JsonObject> bodyParts() {
		return bodyParts( GsonJsonHandlers.object() );
	}

	@Override
	public <T> List<T> bodyParts(JsonHandler<T> handler) {
		Objects.requireNonNull( Objects.requireNonNull( handler ).javaType() );
		return request.bodyPartsJson().stream()
				.map( part -> JsonHandlerBridge.exportObject( handler, part, "schema export", userFacingCodec.validation() ) )
				.toList();
	}

}
