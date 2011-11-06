/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.engine.spi;

/**
 * Used to check the constraints of depth when using {@link org.hibernate.search.annotations.IndexedEmbedded}
 * or {@link org.hibernate.search.annotations.ContainedIn} annotations.
 *
 * @author Davide D'Alto
 */
public class DepthValidator {

	private int maxDepth;
	private int depth;

	public DepthValidator(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void increaseDepth() {
		depth++;
	}

	public boolean isMaxDepthReached() {
		return depth > maxDepth;
	}

	public boolean isMaxDepthInfinite() {
		return maxDepth == Integer.MAX_VALUE;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	@Override
	public String toString() {
		return "[maxDepth=" + maxDepth + ", level=" + depth + "]";
	}
}
