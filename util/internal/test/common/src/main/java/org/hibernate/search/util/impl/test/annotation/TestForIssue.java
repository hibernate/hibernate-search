/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A documentation annotation for notating what JIRA issue is being tested.
 *
 * @author Steve Ebersole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface TestForIssue {
	/**
	 * The keys of the JIRA issues tested.
	 *
	 * @return The JIRA issue keys
	 */
	String[] jiraKey();
}
