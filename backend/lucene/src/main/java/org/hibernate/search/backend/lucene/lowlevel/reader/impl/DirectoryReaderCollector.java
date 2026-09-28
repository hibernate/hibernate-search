package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import org.apache.lucene.index.DirectoryReader;

public interface DirectoryReaderCollector {

	void collect(String mappedTypeName, DirectoryReader directoryReader);

}
