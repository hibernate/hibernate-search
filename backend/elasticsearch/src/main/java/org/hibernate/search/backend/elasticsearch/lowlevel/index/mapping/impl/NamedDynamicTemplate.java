/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;

/**
 * An object representing an Elasticsearch dynamic template with a name.
 * <p>
 * This is the outer object in the array of templates, wrapping a {@link DynamicTemplate}.
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html
 */
@JsonAdapter(NamedDynamicTemplateJsonAdapterFactory.class)
public class NamedDynamicTemplate {

	public final String name;

	public final DynamicTemplate template;

	public NamedDynamicTemplate(String name, DynamicTemplate template) {
		this.name = name;
		this.template = template;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

	public String getName() {
		return name;
	}

	public DynamicTemplate getTemplate() {
		return template;
	}
}
