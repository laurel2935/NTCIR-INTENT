/** BM25 Kernel
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.util.DocUtils;
import org.archive.util.VectorUtils;



// TODO: Kernel should really get whole content store, then just references to doc names
//       MMR, etc., should not have to pass around kernel representation
public class BM25Kernel extends Kernel {

	public final static boolean DEBUG = false;
	
	/* k1 \in [0,inf], k1=0 => standard doc TF, k1 --> inf reduces effect of doc TF */
	public double _k1 = -1d; 
	
	/* k3 \in [0,inf], k3=0 => standard query TF, k3 --> inf reduces effect of query TF */
	public double _k3 = -1d; 
	
	/* b  \in [0,1], b=0 => no doc length effect, b=1 => full penalty \propto doc_length / avg */
	public double _b  = -1d; 
	
	
	/**query-specific**/
	public double  _dDefaultIDF = -1d;
	public double  _dAvgDocLength = -1d;
	public Map<Object, Double> _hmKey2IDF = null;
	public Set<String> _mPrevInit = null;
	
	public BM25Kernel(HashMap<String,String> docs, 
			double k1, double k3, double b) {
		super(docs);
		_k1 = k1;
		_k3 = k3;
		_b  = b;
	}

	public void clearInfoOfTopNDocs() {
		_dDefaultIDF = -1d;
		_dAvgDocLength = -1d;
		_hmKey2IDF = null;
		_mPrevInit = null;	
	}
	
	public void initTonNDocs(Set<String> docs_topn) {
		if (docs_topn == _mPrevInit)
			return; // Already initialized
		
		_mPrevInit = docs_topn;
		_hmKey2IDF = new HashMap<Object,Double>();
		double total_doc_length = 0d;
		int i = 0;
		
		// Have to go through all documents to compute term IDF
		for (String doc : docs_topn) {
			Map<Object,Double> features = (Map<Object,Double>)getObjectRepresentation(doc);
			features = VectorUtils.ConvertToBoolean(features);
			total_doc_length += VectorUtils.L1Norm(features);
			_hmKey2IDF = VectorUtils.Sum(_hmKey2IDF, features);
			if (false && ++i % 100 == 0) {
				System.out.println("- Converted " + i + " documents to internal representation"); 
				System.out.flush();
			}
		}
		_dAvgDocLength = total_doc_length / (double)docs_topn.size();
		
		for (Object key : _hmKey2IDF.keySet()) {
			Double idf = (docs_topn.size() + 1d)/ (_hmKey2IDF.get(key) + 1d);
			_hmKey2IDF.put(key, Math.log(idf));
		}
		_dDefaultIDF = Math.log((docs_topn.size() + 1d) / 1d);
		if (DEBUG) {
			System.out.println("Avg doc length: " + _dAvgDocLength);
			System.out.println("Default IDF   : " + _dDefaultIDF);
			System.out.println("IDF after log:  " + _hmKey2IDF);
		}
	}

	public Object getNoncachedObjectRepresentation(String content) {
		Map<Object,Double> features = DocUtils.ConvertToFeatureMap(content);
		return features;
	}
	
	/** Note: the BM25 kernel places interpretations on the two objects
	 *        compared... query should always come first.
	 */
	public double sim(Object o1, Object o2) {
		
		// Will modify in place, so need to copy
		Map<Object, Double> query = VectorUtils.Copy((Map<Object, Double>)o1);
		Map<Object, Double> doc   = VectorUtils.Copy((Map<Object, Double>)o2);
		
		// Modify query according to BM25 kernel
		for (Object key : query.keySet()) {
			double freq = query.get(key);
			freq = ((_k3 + 1) * freq) / (_k3 + freq);
			Double idf  = _hmKey2IDF.get(key);
			if (idf == null)
				idf = _dDefaultIDF;
			query.put(key, freq * idf);
		}
		
		// Modify doc according to BM25 kernel
		double doc_length_ratio = VectorUtils.L1Norm(doc) / _dAvgDocLength;
		for (Object key : doc.keySet()) {
			double freq = doc.get(key);
			freq = ((_k1 + 1) * freq) / (_k1 * (1d - _b + _b*doc_length_ratio) + freq);
			doc.put(key, freq);
		}
		
		return VectorUtils.DotProduct(query, doc);
	}

	public double sim(Object o1, Object o2, Object ow) {
		System.out.println("ERROR: Cannot do BM25 query-reweighted similarity");
		System.exit(1);
		return -1d;
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}

	@Override
	public String getKernelDescription() {
		return "BM25-Kernel (k1=" + _k1 + ", k3=" + _k3 + ", b=" + _b + ")";
	}
	
	public String getString(){
		return "BM25Kernel";
	}

}
