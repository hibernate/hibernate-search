/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl.factory;

import java.util.List;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.builder.BulkWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ClearScrollWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.CloseIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.CreateIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DropIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ExplainWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexTypeMappingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexSettingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexExistsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.OpenIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.PutIndexMappingWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.PutIndexSettingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ScrollWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.WaitForIndexStatusWorkBuilder;
import org.hibernate.search.engine.service.spi.Service;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkFactory extends Service {

	IndexWorkBuilder index(String indexName, String typeName, String id, JsonObject document);

	DeleteWorkBuilder delete(String indexName, String typeName, String id);

	DeleteByQueryWorkBuilder deleteByQuery(String indexName, JsonObject payload);

	FlushWorkBuilder flush();

	RefreshWorkBuilder refresh();

	OptimizeWorkBuilder optimize();

	BulkWorkBuilder bulk(List<BulkableElasticsearchWork<?>> bulkableWorks);

	SearchWorkBuilder search(String payload);

	ExplainWorkBuilder explain(String indexName, String typeName, String id, JsonObject payload);

	ScrollWorkBuilder scroll(String scrollId, String scrollTimeout);

	ClearScrollWorkBuilder clearScroll(String scrollId);

	CreateIndexWorkBuilder createIndex(String indexName);

	DropIndexWorkBuilder dropIndex(String indexName);

	OpenIndexWorkBuilder openIndex(String indexName);

	CloseIndexWorkBuilder closeIndex(String indexName);

	IndexExistsWorkBuilder indexExists(String indexName);

	GetIndexSettingsWorkBuilder getIndexSettings(String indexName);

	PutIndexSettingsWorkBuilder putIndexSettings(String indexName, IndexSettings settings);

	GetIndexTypeMappingsWorkBuilder getIndexTypeMappings(String indexName);

	PutIndexMappingWorkBuilder putIndexTypeMapping(String indexName, String typeName, TypeMapping mapping);

	WaitForIndexStatusWorkBuilder waitForIndexStatusWork(String indexName, ElasticsearchIndexStatus requiredStatus, String timeout);

}
