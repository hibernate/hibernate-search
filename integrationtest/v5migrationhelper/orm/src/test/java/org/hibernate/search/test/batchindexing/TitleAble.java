/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

public interface TitleAble {

	String getTitle();

	void setTitle(String title);

	void setFirstPublishedIn(Nation firstPublishedIn);

	Nation getFirstPublishedIn();

}
