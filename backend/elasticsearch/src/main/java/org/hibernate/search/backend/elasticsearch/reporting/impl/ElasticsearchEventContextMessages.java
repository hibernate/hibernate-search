/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.reporting.impl;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the Elasticsearch backend.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchEventContextMessages {

	ElasticsearchEventContextMessages INSTANCE = Messages.getBundle( ElasticsearchEventContextMessages.class );

	@Message(value = "attribute '%1$s'")
	String mappingAttribute(String name);

	@Message(value = "parameter '%1$s'")
	String analysisDefinitionParameter(String name);

	@Message(value = "alias '%1$s'")
	String aliasDefinition(String name);

	@Message(value = "attribute '%1$s'")
	String analysisDefinitionAttribute(String name);

	@Message(value = "attribute '%1$s'")
	String fieldTemplateAttribute(String name);

	@Message(value = "attribute '%1$s'")
	String customIndexSettingAttribute(String name);

	@Message(value = "attribute '%1$s'")
	String customIndexMappingAttribute(String name);
}
