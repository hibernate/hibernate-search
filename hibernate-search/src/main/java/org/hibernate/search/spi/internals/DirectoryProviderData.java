package org.hibernate.search.spi.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.search.Similarity;

import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
* @author Emmanuel Bernard
*/
public class DirectoryProviderData {
	private final ReentrantLock dirLock = new ReentrantLock();
	private OptimizerStrategy optimizerStrategy;
	private final Set<Class<?>> classes = new HashSet<Class<?>>( 2 );
	private Similarity similarity = null;
	private boolean exclusiveIndexUsage;

	public void setOptimizerStrategy(OptimizerStrategy optimizerStrategy) {
		this.optimizerStrategy = optimizerStrategy;
	}

	public void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
	}

	public void setExclusiveIndexUsage(boolean exclusiveIndexUsage) {
		this.exclusiveIndexUsage = exclusiveIndexUsage;
	}

	public ReentrantLock getDirLock() {

		return dirLock;
	}

	public OptimizerStrategy getOptimizerStrategy() {
		return optimizerStrategy;
	}

	public Set<Class<?>> getClasses() {
		return classes;
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	public boolean isExclusiveIndexUsage() {
		return exclusiveIndexUsage;
	}
}
