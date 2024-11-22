/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;
import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET_LEGACY_ENGINE;

import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.SimpleNameClassFormatter;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = FormattingLog.CATEGORY_NAME,
		description = "Logs related to parsing/formatting."
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface FormattingLog {
	String CATEGORY_NAME = "org.hibernate.search.formatting.mapper";

	FormattingLog INSTANCE = LoggerFactory.make( FormattingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 295, value = "Invalid value for type '$2%s': '$1%s'. %3$s")
	SearchException parseException(String text, @FormatWith(SimpleNameClassFormatter.class) Class<?> readerClass,
			String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 297, value = "Unable to convert '%2$s' into type '%1$s': value is too large.")
	SearchException valueTooLargeForConversionException(@FormatWith(SimpleNameClassFormatter.class) Class<?> type,
			Object duration, @Cause Exception ae);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 43, value = "Exception creating URL from String '%1$s'.")
	SearchException malformedURL(String value, @Cause Exception e);

	@Message(id = ID_OFFSET + 44, value = "Exception creating URI from String '%1$s'.")
	SearchException badURISyntax(String value, @Cause URISyntaxException e);

	@Message(id = ID_OFFSET + 158,
			value = "A non-string tenant ID '%1$s' cannot be used with a default TenantIdentifierConverter. "
					+ "Provide your custom implementation of TenantIdentifierConverter to use non-string tenant identifiers.")
	SearchException nonStringTenantId(Object tenantId);
}
