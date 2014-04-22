/** LDA Kernel
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.ml.lda.LDAInterface;
import org.archive.util.DocUtils;
import org.archive.util.VectorUtils;


public class LDAKernel extends Kernel {

	public final static boolean DEBUG = false;
	public final static long    RAND_SEED = 123456;
	
	public Integer _nTopics = -1;
	public double _dAlpha = -1d;
	public double _dBeta  = -1d;
	public Boolean _bSpherical = false;
	public boolean _bReweightedSimilarity = false;
	
	/**query-specific**/
	public Set<String> _mPrevInit = null;
	public Map<String, double[]> _doc2topicVector = null;
	public Map<Integer,Integer> _hmHashCode2DocID = null;
	public LDAInterface _lda = new LDAInterface();
	
	// Now automatically set: double alpha, double beta,
	public LDAKernel(HashMap<String,String> docs, 
			int num_topics, boolean spherical, boolean weighted_similarity) {
		super(docs);
		_nTopics = num_topics;
		_dAlpha  = 2.0d;
		_dBeta   = 0.5d;
		_bSpherical = spherical;
		// Do we allow a diversity measure that is reweighted by the query?
		_bReweightedSimilarity = weighted_similarity;
	}
	
	public void clearInfoOfTopNDocs() {
		_mPrevInit = null;
		_doc2topicVector = null;
		_hmHashCode2DocID = null;
		_lda = new LDAInterface();
	}

	@Override
	public Object getNoncachedObjectRepresentation(String content) {
		
		double[] topic_vector = null;
		
		// See if we can extract the content representation directly from LDA
		int hash_code = content.hashCode();
		Integer m = _hmHashCode2DocID.get(hash_code);
		if (m != null && m >= 0 && m < _lda._M) {
			
			// Document m found
			topic_vector = new double[_lda._K];
			for (int k = 0; k < _lda._K; k++)
				topic_vector[k] = _lda._theta[m][k];
			
		} else {
			// Document m not found, use "folding-in" trick to estimate topic_vector
			topic_vector = _lda.getNewDocTopics(content);
		}
		
		if (DEBUG) {
			if (content.length() > 100)
				content = content.substring(0,100);
			System.out.println("Content: " + content);
			for (int k = 0; k < _nTopics; k++)
				System.out.println("- " + DocUtils.DF3.format(topic_vector[k]) + ": " + _lda.getTopicName(k));
		}
		return topic_vector;
	}

	@Override
	// Because we'll get better topic models from docs that were explicitly
	// added to the LDA model, we retain the hashcode of these documents so
	// we can retrieve their direct LDA topic vectors from the LDA Theta 
	// matrix.  There is a small chance of a hashcode collision, but we'll
	// accept this small chance of error in favor of not having to do
	// a String equality test on the entire document.
	public void initTonNDocs(Set<String> docs_topn) {
		
		if (docs_topn == _mPrevInit)
			return; // Already initialized
		_mPrevInit = docs_topn;

		_hmHashCode2DocID = new HashMap<Integer, Integer>();
		for (String doc : docs_topn) {
			String content = _docs_all.get(doc);
			int doc_id = _lda.addDocument(DocUtils.Tokenize(content));
			int hash_code = content.hashCode(); // TODO: This is horrible, should be name ref!
			_hmHashCode2DocID.put(hash_code, doc_id);
		}
		//_lda.infer(_nTopics, _dAlpha, _dBeta, RAND_SEED);
		_lda.infer(_nTopics, _dAlpha, _dBeta, RAND_SEED);
		//if (DEBUG)
		//	System.out.println("LEARNED TOPICS:\n===============\n" + _lda);
		
		// TODO: Replace cache contents since it changes with every run of LDA
		//       (different document set)... MMR should always just reference
		//       by doc name.
		for (String doc : docs_topn) {
			String content = _docs_all.get(doc);
			_reprCache.put(doc, getNoncachedObjectRepresentation(content));
		}
	}

	@Override
	public double sim(Object s1, Object s2) {
		double[] d1 = (double[])s1;
		double[] d2 = (double[])s2;
		if (_bSpherical)
			return VectorUtils.CosSim(d1, d2);
		else
			return VectorUtils.DotProduct(d1, d2);
	}

	@Override
	public double sim(Object s1, Object s2, Object q) {
		double[] d1 = (double[])s1;
		double[] d2 = (double[])s2;
		if (_bReweightedSimilarity) {
			double[] w = (double[])q;
			if (_bSpherical)
				return VectorUtils.WeightedCosSim(d1, d2, w);
			else
				return VectorUtils.WeightedDotProduct(d1, d2, w);
		} else
			return sim(s1, s2);
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		StringBuilder sb = new StringBuilder();
		double[] topic_vector = (double[])obj; 
		for (int k = 0; k < _nTopics; k++)
			sb.append("\n- " + DocUtils.DF3.format(topic_vector[k]) + ": " + _lda.getTopicName(k));
		return sb.toString();
	}

	@Override
	public String getKernelDescription() {
		return "LDAKernel: reweighted_sim=" + _bReweightedSimilarity + 
			", #topics=" + _nTopics + ", alpha=" + _dAlpha + 
			", beta=" + _dBeta + ", spherical=" + _bSpherical;
	}
	public String getString(){
		return "LDAKernel_"+"Spherical["+_bSpherical.toString()+"]_"+"TopicN["+_nTopics.toString()+"]";
	}	
}
