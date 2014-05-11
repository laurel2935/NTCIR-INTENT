/** Abstract Class for a generic ResultList Selector...
 *  subclasses must implement the abstract methods.  The subclass MMR implements
 *  a greedy result list selection method, but other subclasses might
 *  choose other approaches (e.g., global optimization).
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.ranker;

import java.util.*;

import org.archive.dataset.trec.query.TRECDivQuery;

/**
 * ResultRanker runs per query by refreshing "_docTopN" each time.
 * thus, per query, call: addDoc() & initDocs() & clearDocs()
 * thus, subclasses must implements these methods.
 * */
public abstract class ResultRanker {

	public static boolean SHOW_DEBUG = false;
	
	public int _indexOfGetResultMethod = 0;
	
	//doc_name & doc_content, commonly the total doc for all queries
	public Map<String, String> _docs_all     = new HashMap<String, String>();
	//two kinds of representations
	//for sim doc_name, for tfidf, object is term and its frequency
	public Map<String, Object> _docRepr  = new HashMap<String, Object>();
	//for div
	public Map<String, Object> _docRepr2 = new HashMap<String, Object>();
	//set of doc_name with respect to a specific query
	public Set<String>         _docs_topn  = new HashSet<String>();

	public ResultRanker(HashMap<String, String> docs_all) {
		_docs_all = docs_all;
	}
	
	public abstract ArrayList<String> getResultList(String query, int list_size);
	
	public abstract ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int list_size);

	public abstract void addATopNDoc(String doc_name);

	public abstract void initTonNDocsForInnerKernels();

	public abstract void clearInfoOfTopNDocs();

	public abstract String getDescription();
	
	public abstract String getString();
	
	public String getDoc(String doc_name) {
		return _docs_all.get(doc_name);
	}
}
