/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.reporting.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the POJO mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface PojoEventContextMessages {

	PojoEventContextMessages INSTANCE = Messages.getBundle( PojoEventContextMessages.class );

	@Message(value = "Schema management")
	String schemaManagement();

	@Message(value = "constructor with parameter types %1$s")
	String constructor(@FormatWith(CommaSeparatedClassesFormatter.class) Class<?>[] parameterTypes);

	@Message(value = "parameter at index %1$s (%2$s)")
	String methodParameter(int index, String name);

	@Message(value = "<unknown name>")
	String unknownName();

	@Message(value = "path '%1$s'")
	String path(String pathString);

	@Message(value = "annotation '%1$s'")
	String annotation(String annotationString);

	@Message(value = "annotation type '@%1$s'")
	String annotationType(@FormatWith(ClassFormatter.class) Class<? extends Annotation> annotationType);

}
