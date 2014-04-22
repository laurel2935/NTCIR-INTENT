package org.archive.a1.ranker.fa;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEval10Losses;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.util.DevNullPrintStream;
import org.archive.util.Pair;
import org.archive.util.VectorUtils;
import org.archive.util.tuple.PairComparatorBySecond_Desc;

/**
 * A common framework which essentially a global optimization!
 * Type-1: argmax{ f(S)=lambda*r(S)+(1-lambda)*d(S)	},
 * 	where r(S) represents the entire relevance, d(S) represents the diversity
 * **/

public class MDP extends ResultRanker {
	private Map<String,TREC68Query> _allTREC68Queries = null; 
	private Map<String,TRECDivQuery> _allTRECDivQueries = null;
	//
	
	public double _dLambda;
	//subtopic kernel, each point is represented as a subtopic vector
	public Kernel _sbKernel;	
	//buffer similarity values by _sbKernel for two items
	public HashMap<Pair,Double>   _simCache;
	//buffer distance values by _sbKernel for two items
	public HashMap<Pair,Double>   _disCache;
	
	// Constructor
	public MDP(HashMap<String, String> docs, double lambda, Kernel sbKernel,
			Map<String,TREC68Query> allTREC68Queries, Map<String,TRECDivQuery> allTRECDivQueries) { 
		super(docs);		
		this._dLambda = lambda;
		this._sbKernel = sbKernel;		
		this._simCache = new HashMap<Pair,Double>();		
		this._disCache = new HashMap<Pair,Double>();	
		//
		if(null != allTREC68Queries){
			this._allTREC68Queries = allTREC68Queries;
		}
		if(null != allTRECDivQueries){
			this._allTRECDivQueries = allTRECDivQueries;
		}		
	}
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	//refresh each time for a query
	//_docOrig, i.e., the top-n set of a query
	public void clearInfoOfTopNDocs() {
		_docRepr.clear();		
		_docs_topn.clear();
		_sbKernel.clearInfoOfTopNDocs();		
		// No need to clear sim and div caches, these are local and conditioned
		// on query and we expect that the sim and div kernels will not change.
	}
	//called when a new query come
	public void initTonNDocsForInnerKernels() {
		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_sbKernel.initTonNDocs(_docs_topn); // LDA should maintain keys for mapping later		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		for (String doc : _docs_topn) {
			Object repr = _sbKernel.getObjectRepresentation(doc);
			_docRepr.put(doc, repr);			
		}
	}
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	//
	public ArrayList<String> getResultList(String query, int size, fVersion _fVersion) {		
		// Get representation for query
		Object query_repr = _sbKernel.getNoncachedObjectRepresentation(query);
		//
		ArrayList<org.archive.util.tuple.Pair<String, Double>> rankedS = 
			hillClimbingSearch(query_repr, _docs_topn, size, _fVersion);		
		//
		ArrayList<String> result_list = new ArrayList<String>();
		for(int i=0; i<rankedS.size(); i++){
			result_list.add(rankedS.get(i).getFirst());
		}
		//
		return result_list;		
	}
	//////////////////////////
	//Optimizing Functions f by Maximizing f(S)
	//////////////////////////
	private double f(Object query_repr, Set<String> S, Set<String> D_Minus_S, fVersion _fVersion){
		if(_fVersion == fVersion._dfa){
			return f_dfa(query_repr, S, D_Minus_S);		
		}else if(_fVersion == fVersion._dfa_scaled){
			return f_dfa_scaled(query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._md){
			return f_md(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._md_scaled){
			return f_md_scaled(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa){
			return f_pdfa(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled){
			return f_pdfa_scaled(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp){
			return f_pdfa_scaled_exp(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp_head){
			return f_pdfa_scaled_exp_head(query_repr, D_Minus_S);
		}else{
			System.out.println("ERROR: unexpected fVersion!");
			new Exception().printStackTrace();
			System.exit(1);
			return -1d;
		}		
	}
	//////////////////////////
	//Contribution of each doc of S: c()
	//////////////////////////
	private double c(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S, fVersion _fVersion){
		if(_fVersion == fVersion._dfa){
			return c_dfa(doc_name_k, query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._dfa_scaled){
			return c_dfa_scaled(doc_name_k, query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._md){
			return c_md(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._md_scaled){
			return c_md_scaled(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa){
			return c_pdfa(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled){
			return c_pdfa_scaled(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp){
			return c_pdfa_scaled_exp(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp_head){
			return c_pdfa_scaled_exp_head(doc_name_k, query_repr, D_Minus_S);
		}else{
			System.out.println("ERROR: unexpected fVersion!");
			new Exception().printStackTrace();
			System.exit(1);
			return -1d;
		}
	}
	//	f versions	//
	public static enum fVersion {_dfa, _dfa_scaled, _md, _md_scaled, 
		_pdfa, _pdfa_scaled, _pdfa_scaled_exp, _pdfa_scaled_exp_head}
	//dfa function: desirable facilities allocation ()
	private double f_dfa(Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*getSumRelevance(query_repr, S)
			+(1 - _dLambda)*(-calDiversity_DFA(S, D_Minus_S));
	}	
	//dfa_scaled function: desirable facilities allocation
	private double f_dfa_scaled(Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*(-calDiversity_DFA_Averaged(S, D_Minus_S));		
	}
	//contribution per doc
	private double c_dfa(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(-dContributionOfK_dfa(doc_name_k, S, D_Minus_S));
	}
	private double c_dfa_scaled(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(1d/D_Minus_S.size())*(-dContributionOfK_dfa(doc_name_k, S, D_Minus_S));
	}
	//md function: merely maximizing dispersion
	private double f_md(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance(query_repr, S)
		+(1-_dLambda)*calDiversity_DISP(S);
	}
	//md_scaled function: merely maximizing dispersion, averaged
	private double f_md_scaled(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*calDiversity_DISP_Averaged(S);
	}	
	//contribution per doc
	private double c_md(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	private double c_md_scaled(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	//pdfa function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa)
	private double f_pdfa(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance(query_repr, S)
			+(1-_dLambda)*(calDiversity_DISP(S)/calProportionality(query_repr, S));
	}
	//pdfa_scaled function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa) scaled
	private double f_pdfa_scaled(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*(calDiversity_DISP_Averaged(S)/calProportionality_Scaled(query_repr, S));
	}
	//
	private double c_pdfa(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(dContributionOfKGivenS_disp(doc_name_k, S)/pContributionOfK(doc_name_k, query_repr));
	}
	private double c_pdfa_scaled(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
		+(1-_dLambda)*((1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S)/pContributionOfK_scaled(doc_name_k, query_repr));
	}
	//f_pdfa_scaled_exp function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa) scaled and exp
	private double f_pdfa_scaled_exp(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*calDiversity_DISP_Averaged(S)*Math.exp(1-calProportionality_Scaled(query_repr, S));
	}
	private double c_pdfa_scaled_exp(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*((1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S))*Math.exp(1-pContributionOfK_scaled(doc_name_k, query_repr));
	}
	//f_pdfa_scaled_exp_head function: query-oriented dispersion
	private double f_pdfa_scaled_exp_head(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)*Math.exp(1-calProportionality_Scaled(query_repr, S))
			+(1-_dLambda)*calDiversity_DISP_Averaged(S);
	}
	//
	private double c_pdfa_scaled_exp_head(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)*Math.exp(1-pContributionOfK_scaled(doc_name_k, query_repr))
			+(1-_dLambda)*(1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	
	//////////////////////////
	//HillClimbingSearch
	//////////////////////////	
	//(1)
	private ArrayList<org.archive.util.tuple.Pair<String, Double>> hillClimbingSearch(
			Object query_repr, Set<String> D, int k, fVersion _fVersion){
		//
		ArrayList<org.archive.util.tuple.Pair<String, Double>> topnDoc_rlist =
			new ArrayList<org.archive.util.tuple.Pair<String, Double>>();
		//
		String query_repr_key = query_repr.toString();
		Double sim_score = null;
		Object doc_repr = null;
		Pair sim_key = null;
		//
		for(String doc_name: D){
			sim_key = new Pair(doc_name, query_repr_key);
			if (null == (sim_score = _simCache.get(sim_key))) {
				doc_repr = _docRepr.get(doc_name);
				sim_score = _sbKernel.sim(doc_repr, query_repr);
				_simCache.put(sim_key, sim_score);
			}
			//
			topnDoc_rlist.add(new org.archive.util.tuple.Pair<String, Double>(doc_name, sim_score));
		}
		//
		Collections.sort(topnDoc_rlist, new PairComparatorBySecond_Desc<String, Double>());
		//
		HashSet<String> S = new HashSet<String>();
		//initialize with top-k relevant doc
		for(int i=0; i<k; i++){
			S.add(topnDoc_rlist.get(i).getFirst());
		}
		//
		Set<String> D_Minus_S = new HashSet<String>();
		Iterator<String> dItr = D.iterator();
		while(dItr.hasNext()){
			String doc_name = dItr.next();
			if(!S.contains(doc_name)){
				D_Minus_S.add(doc_name);
			}
		}
		//		
		HashSet<String> old_S = new HashSet<String>();	
		HashSet<String> old_D_Minus_S = new HashSet<String>();		
		old_S.addAll(S);			
		old_D_Minus_S.addAll(D_Minus_S);
		//		
		boolean change;
		int count = 0;
		do{	
			change = false;
			if(count > 100000){
				System.out.println("Maximum Itration!");
				break;
			}else {
				count++;
			}			
			//			
			for(String doc_in: old_S){				
				for(String doc_out: old_D_Minus_S){					
					//exchange for S
					S.remove(doc_in);
					S.add(doc_out);
					//exchange for D_Minus_S
					D_Minus_S.remove(doc_out);
					D_Minus_S.add(doc_in);
					//
					if(f(query_repr, S, D_Minus_S, _fVersion) > f(query_repr, old_S, old_D_Minus_S, _fVersion)){
						//change like S
						old_S.remove(doc_in);
						old_S.add(doc_out);
						//change like D_Minus_S
						old_D_Minus_S.remove(doc_out);
						old_D_Minus_S.add(doc_in);
						//
						change = true;
						break;
					}else{
						//back
						S.remove(doc_out);
						S.add(doc_in);
						//
						D_Minus_S.remove(doc_in);
						D_Minus_S.add(doc_out);
					}
				}
				//
				if(change){
					break;
				}
			}			
		}while(change);
		//sorting in order of contribution
		ArrayList<org.archive.util.tuple.Pair<String, Double>> s_c_list =
			new ArrayList<org.archive.util.tuple.Pair<String, Double>>();
		Iterator<String> sItr = S.iterator();
		//
		while(sItr.hasNext()){
			String doc_name_k = sItr.next();			
			//
			s_c_list.add(new org.archive.util.tuple.Pair<String, Double>(doc_name_k, 
					c(doc_name_k, query_repr, S, D_Minus_S, _fVersion)));
		}
		//
		Collections.sort(s_c_list, new PairComparatorBySecond_Desc<String, Double>());
		//
		return s_c_list;
	}
	
	//(2)
	private ArrayList<org.archive.util.tuple.Pair<String, Double>> hillClimbingSearch_dfa(
			Object query_repr, Set<String> D, int k, fVersion _fVersion){
		//
		ArrayList<org.archive.util.tuple.Pair<String, Double>> topnDoc_rlist =
			new ArrayList<org.archive.util.tuple.Pair<String, Double>>();
		//
		String query_repr_key = query_repr.toString();
		Double sim_score = null;
		Object doc_repr = null;
		Pair sim_key = null;
		
		for(String doc_name: D){
			sim_key = new Pair(doc_name, query_repr_key);
			if (null == (sim_score = _simCache.get(sim_key))) {
				doc_repr = _docRepr.get(doc_name);
				sim_score = _sbKernel.sim(doc_repr, query_repr);
				_simCache.put(sim_key, sim_score);
			}
			
			topnDoc_rlist.add(new org.archive.util.tuple.Pair<String, Double>(doc_name, sim_score));
		}
		
		Collections.sort(topnDoc_rlist, new PairComparatorBySecond_Desc<String, Double>());
		//
		ArrayList<String> S = new ArrayList<String>();
		//initialize with top-k relevant doc
		for(int i=0; i<k; i++){
			S.add(topnDoc_rlist.get(i).getFirst());
		}
		//
		ArrayList<String> D_Minus_S = new ArrayList<String>();
		//star: elements of S, follower: elements of D_Minus_S
		//star->followers & value		
		HashMap<String, HashSet<org.archive.util.tuple.Pair<String, Double>>> starToFollowersMap = 
			new HashMap<String, HashSet<org.archive.util.tuple.Pair<String, Double>>>();
		
		HashMap<String, org.archive.util.tuple.Pair<String, Double>> followerToStarMap =
			new HashMap<String, org.archive.util.tuple.Pair<String,Double>>();
		
		org.archive.util.tuple.Pair<String, Double> minPair = null;
		Iterator<String> dItr = D.iterator();		
		while(dItr.hasNext()){
			String doc_name = dItr.next();
			if(!S.contains(doc_name)){
				D_Minus_S.add(doc_name);
				//--
				minPair = minDisPairGivenS(doc_name, S);
				//
				followerToStarMap.put(doc_name, 
						new org.archive.util.tuple.Pair<String, Double>(minPair.getFirst(), minPair.getSecond()));
				//
				if(starToFollowersMap.containsKey(minPair.getFirst())){
					starToFollowersMap.get(minPair.getFirst()).add(
							new org.archive.util.tuple.Pair<String, Double>(doc_name, minPair.getSecond()));
				}else{
					HashSet<org.archive.util.tuple.Pair<String, Double>> follwerSet = 
						new HashSet<org.archive.util.tuple.Pair<String,Double>>();
					
					follwerSet.add(new org.archive.util.tuple.Pair<String, Double>(doc_name, minPair.getSecond()));
					starToFollowersMap.put(minPair.getFirst(), follwerSet);
				}				
			}
		}
		//
		Random random = new Random();
		int starIndex = random.nextInt()%S.size();
		int flagIndex = starIndex;
		boolean change = false;
		do{
			String star = S.get(starIndex);
			change = false;
			//
			ArrayList<String> currentS = new ArrayList<String>();
			currentS.addAll(S);
			currentS.remove(star);
			//
			for(int followerIndex = 0; followerIndex<D_Minus_S.size(); followerIndex++){
				String toBeStar = D_Minus_S.get(followerIndex);
				//
				double delta = 0.0d;
				boolean edged = false;
				//newly obtained S
				currentS.add(D_Minus_S.get(followerIndex));
				//follower->new-star & distance value
				HashMap<String, org.archive.util.tuple.Pair<String, Double>> moveFollowersMap = 
					new HashMap<String, org.archive.util.tuple.Pair<String,Double>>();
				//former information deletion due to role change of boBeStar				
				//--
				//doc_in: from star to follower
				HashSet<org.archive.util.tuple.Pair<String, Double>> follwerSet = starToFollowersMap.get(star);
				Iterator<org.archive.util.tuple.Pair<String, Double>> fItr = follwerSet.iterator();
				while(fItr.hasNext()){
					org.archive.util.tuple.Pair<String, Double> follower = fItr.next();
					if(follower.getFirst() == toBeStar){
						delta += (0-follower.getSecond());
						edged = true;
					}else{
						org.archive.util.tuple.Pair<String, Double> newMinPair = 
							minDisPairGivenS(follower.getFirst(), currentS);
						//
						delta += (newMinPair.getSecond()-follower.getSecond());
						//
						moveFollowersMap.put(follower.getFirst(),
								new org.archive.util.tuple.Pair<String,Double>(newMinPair.getFirst(), newMinPair.getSecond()));
					}
				}
				//for toBeStar
				if(!edged){
					delta += (0-followerToStarMap.get(toBeStar).getSecond());					
				}
				//for star
				org.archive.util.tuple.Pair<String, Double> newMinPair = minDisPairGivenS(star, currentS);
				delta += newMinPair.getSecond();
				//
				moveFollowersMap.put(star,
						new org.archive.util.tuple.Pair<String,Double>(newMinPair.getFirst(), newMinPair.getSecond()));
				//delete of ...
				if(delta < 0){					
					if(!edged){
						starToFollowersMap.get(followerToStarMap.get(toBeStar).getFirst()).remove(toBeStar);						
					}
					starToFollowersMap.remove(star);
					followerToStarMap.remove(toBeStar);
					//
					for(Entry<String, org.archive.util.tuple.Pair<String, Double>> entry:
						moveFollowersMap.entrySet()){
						//starToFollowersMap
						if(starToFollowersMap.containsKey(entry.getValue().getFirst())){
							starToFollowersMap.get(entry.getValue().getFirst()).add(
									new org.archive.util.tuple.Pair<String, Double>(entry.getKey(), entry.getValue().getSecond()));
						}else{
							HashSet<org.archive.util.tuple.Pair<String, Double>> followerSet = 
								new HashSet<org.archive.util.tuple.Pair<String,Double>>();
							followerSet.add(new org.archive.util.tuple.Pair<String,Double>(entry.getKey(), entry.getValue().getSecond()));
							starToFollowersMap.put(entry.getValue().getFirst(), followerSet);
						}
						//followerToStarMap
						followerToStarMap.put(entry.getKey(), 
								new org.archive.util.tuple.Pair<String, Double>(entry.getValue().getFirst(), entry.getValue().getSecond()));
						//						
					}
					//
					S.clear();
					S.addAll(currentS);
					//
					D_Minus_S.remove(followerIndex);
					D_Minus_S.add(star);
					//
					change = true;
					break;
				}				
			}
			//
			if(change){
				starIndex = (starIndex+1)%S.size();
				flagIndex = starIndex;
			}else if((starIndex=(starIndex+1)%S.size()) == flagIndex){
				break;
			}			
		}while(true);
		//--		
		HashSet<String> old_S = new HashSet<String>();	
		HashSet<String> old_D_Minus_S = new HashSet<String>();		
		old_S.addAll(S);			
		old_D_Minus_S.addAll(D_Minus_S);
		//		
		boolean change;
		int count = 0;
		do{	
			change = false;
			if(count > 100000){
				System.out.println("Maximum Itration!");
				break;
			}else {
				count++;
			}			
			//			
			for(String doc_in: old_S){				
				for(String doc_out: old_D_Minus_S){
					//--
					double fitness = 0d;
					boolean linked = false;
					HashMap<String, org.archive.util.tuple.Pair<String, Double>> followerToNewStarMap = 
						new HashMap<String, org.archive.util.tuple.Pair<String,Double>>();
					HashSet<String> tempS = new HashSet<String>();
					tempS.addAll(S);
					tempS.remove(doc_in);
					tempS.add(doc_out);
					//doc_in: from star to follower
					HashSet<org.archive.util.tuple.Pair<String, Double>> follwerSet = starToFollowersMap.get(doc_in);
					Iterator<org.archive.util.tuple.Pair<String, Double>> sItr = follwerSet.iterator();
					while(sItr.hasNext()){
						org.archive.util.tuple.Pair<String, Double> follower = sItr.next();
						if(follower.getFirst() == doc_out){
							fitness += (-follower.getSecond());
							linked = true;
						}else{
							org.archive.util.tuple.Pair<String, Double> newMinPair = 
								minDisPairGivenS(follower.getFirst(), tempS);
							//
							fitness += (newMinPair.getSecond()-follower.getSecond());
							//
							followerToNewStarMap.put(follower.getFirst(),
									new org.archive.util.tuple.Pair<String,Double>(newMinPair.getFirst(), newMinPair.getSecond()));
						}
					}
					if(!linked){
						fitness += (-followerToStarMap.get(doc_out).getSecond());
					}
					org.archive.util.tuple.Pair<String, Double> newMinPair = 
						minDisPairGivenS(doc_in, tempS);
					fitness += newMinPair.getSecond();
					followerToNewStarMap.put(doc_in,
							new org.archive.util.tuple.Pair<String,Double>(newMinPair.getFirst(), newMinPair.getSecond()));
					
					
					//doc_out: from follower to star
					//--
					//exchange for S
					S.remove(doc_in);
					S.add(doc_out);
					//exchange for D_Minus_S
					D_Minus_S.remove(doc_out);
					D_Minus_S.add(doc_in);
					//
					if(f(query_repr, S, D_Minus_S, _fVersion) > f(query_repr, old_S, old_D_Minus_S, _fVersion)){
						//change like S
						old_S.remove(doc_in);
						old_S.add(doc_out);
						//change like D_Minus_S
						old_D_Minus_S.remove(doc_out);
						old_D_Minus_S.add(doc_in);
						//
						change = true;
						break;
					}else{
						//back
						S.remove(doc_out);
						S.add(doc_in);
						//
						D_Minus_S.remove(doc_in);
						D_Minus_S.add(doc_out);
					}
				}
				//
				if(change){
					break;
				}
			}			
		}while(change);
		//sorting in order of contribution
		ArrayList<org.archive.util.tuple.Pair<String, Double>> s_c_list =
			new ArrayList<org.archive.util.tuple.Pair<String, Double>>();
		Iterator<String> sItr = S.iterator();
		//
		while(sItr.hasNext()){
			String doc_name_k = sItr.next();			
			//
			s_c_list.add(new org.archive.util.tuple.Pair<String, Double>(doc_name_k, 
					c(doc_name_k, query_repr, S, D_Minus_S, _fVersion)));
		}
		//
		Collections.sort(s_c_list, new PairComparatorBySecond_Desc<String, Double>());
		//
		return s_c_list;
	}
	
	//////////////////////////
	//Relevance Expression
	//////////////////////////
	/**
	 * @return naive sum relevance of S
	 * */
	private double getSumRelevance(Object query_repr, Set<String> S){
		double sumRelevance = 0.0;				
		for(String doc_name: S){			
			sumRelevance += calRelevance(doc_name, query_repr);
		}
		//
		return sumRelevance;		
	}
	//
	private double calRelevance(String doc_name, Object query_repr){
		String query_repr_key = query_repr.toString();
		Double sim_score = null;
		Object doc_repr = null;
		Pair sim_key = null;
		//
		sim_key = new Pair(doc_name, query_repr_key);
		if (null == (sim_score = _simCache.get(sim_key))) {
			doc_repr = _docRepr.get(doc_name);
			sim_score = _sbKernel.sim(doc_repr, query_repr);
			_simCache.put(sim_key, sim_score);
		}
		//
		return sim_score;		
	}
	/**
	 * @return averaged sum-relevance
	 * */
	private double getSumRelevance_Averaged(Object query_repr, Set<String> S){
		return getSumRelevance(query_repr, S)/S.size();
	}
	//////////////////////////
	//Diversity Expression
	//////////////////////////
	//	DFA_Diversity	//
	/**
	 * @return get the diversity derived as desirable facility allocation (DFA)
	 * */
	private double calDiversity_DFA(Set<String> S, Set<String> D_Minus_S){
		double sumDistance = 0.0;
		//
		for(String doc_name_i: D_Minus_S){
			sumDistance += minDisGivenS(doc_name_i, S);
		}
		//
		return sumDistance;		
	}
	/**
	 * @return get the averaged diversity derived as desirable facility allocation (DFA)
	 * */
	private double calDiversity_DFA_Averaged(Set<String> S, Set<String> D_Minus_S) {
		return calDiversity_DFA(S, D_Minus_S)/D_Minus_S.size();
	}
	//
	private double minDisGivenS(String doc_name_i, Set<String> S){
		Object doc_repr_i = _docRepr.get(doc_name_i);
		//
		double minDistance = Double.MAX_VALUE;
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		for(String doc_name_j: S){
			dis_key = new Pair(doc_name_i, doc_name_j);
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _docRepr.get(doc_name_j);
				dis_score = _sbKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
			}
		}
		//
		return minDistance;
	}
	/**
	 * @return the star element with a minimum distance value in S with respect to doc_name_i
	 * **/
	private org.archive.util.tuple.Pair<String, Double> minDisPairGivenS(
			String doc_name_i, ArrayList<String> S){
		
		Object doc_repr_i = _docRepr.get(doc_name_i);
		//
		double minDistance = Double.MAX_VALUE;
		String minDoc_name = null;
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		for(String doc_name_j: S){
			dis_key = new Pair(doc_name_i, doc_name_j);
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _docRepr.get(doc_name_j);
				dis_score = _sbKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
				minDoc_name = doc_name_j;
			}
		}
		//
		return new org.archive.util.tuple.Pair(minDoc_name, minDistance);
	}
	//
	private double dContributionOfK_dfa(String doc_name_k, Set<String> S, Set<String> D_Minus_S){
		double cSum = 0.0;
		Iterator<String> itr = D_Minus_S.iterator();
		while(itr.hasNext()){
			cSum += dContributionOfKGivenS_dfa(doc_name_k, S, itr.next());
		}
		//
		return cSum;		
	}
	private double dContributionOfKGivenS_dfa(String doc_name_k, Set<String> S, String doc_name_i){
		Object doc_repr_i = _docRepr.get(doc_name_i);
		//
		String mindoc_name = null;
		double minDistance = 1;
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		for(String doc_name_j: S){
			dis_key = new Pair(doc_name_i, doc_name_j);
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _docRepr.get(doc_name_j);
				dis_score = _sbKernel.distance(doc_repr_i, doc_repr_j);
				_simCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
				mindoc_name = doc_name_j;
			}
		}
		//
		if(mindoc_name.equals(doc_name_k)){
			return minDistance;
		}else{
			return calDistance(doc_name_i, doc_name_k);
		}		
	}
	//
	private double calDistance(String doc_name_i, String doc_name_j){		
		Double dis_score;		
		Pair dis_key = new Pair(doc_name_i, doc_name_j);
		//
		if (null == (dis_score = _disCache.get(dis_key))) {
			Object doc_repr_i = _docRepr.get(doc_name_i);
			Object doc_repr_j = _docRepr.get(doc_name_j);
			dis_score = _sbKernel.distance(doc_repr_i, doc_repr_j);
			_simCache.put(dis_key, dis_score);
		}
		//
		return dis_score;
	}
	//	Dispersion_Diversity	//
	/**
	 * @return mutually dispersion diversity
	 * */
	private double calDiversity_DISP(Set<String> S){
		double sumDistance = 0.0;
		//
		String [] sArray = S.toArray(new String[0]);		
		Pair dis_key = null;
		Double dis_score;
		Object doc_repr_i = null;
		Object doc_repr_j = null;
		//
		for(int i=0; i<sArray.length; i++){
			String doc_name_i = sArray[i];
			doc_repr_i = _docRepr.get(doc_name_i);
			for(int j=i+1; j<sArray.length; j++){
				String doc_name_j = sArray[j];
				dis_key = new Pair(doc_name_i, doc_name_j);
				//
				if (null == (dis_score=_disCache.get(dis_key))) {
					doc_repr_j = _docRepr.get(doc_name_j);
					dis_score = _sbKernel.distance(doc_repr_i, doc_repr_j);
					_simCache.put(dis_key, dis_score);
				}
				sumDistance += dis_score;
			}
		}
		//
		return sumDistance;
	}
	/**
	 * @return averaged dispersion diversity
	 * */
	private double calDiversity_DISP_Averaged(Set<String> S){
		return calDiversity_DISP(S)/(S.size()*(S.size()-1));
	}
	//
	private double dContributionOfKGivenS_disp(String doc_name_k, Set<String> S){
		double cSum = 0.0;
		Iterator<String> itr = S.iterator();
		while(itr.hasNext()){
			String doc_name = itr.next();
			if(!doc_name.equals(doc_name_k)){
				cSum += calDistance(doc_name, doc_name_k);
			}
		}
		return cSum;
	}
	//	Proportionality	//
	private double calProportionality(Object query_repr, Set<String> S){
		double[] t_q = (double[])query_repr;
		//
		double [] t_S = new double[t_q.length];		
		for(String doc_name_i: S){
			double [] t_i = (double[])_docRepr.get(doc_name_i);
			for(int k=0; k<t_q.length; k++){
				t_S[k] += t_i[k];
			}
		}
		double [] t_S_NormalizeL1 = VectorUtils.NormalizeL1(t_S);
		//
		double sumDivergence = 0.0;
		for(int k=0; k<t_q.length; k++){
			sumDivergence += Math.pow(t_S_NormalizeL1[k]-t_q[k], 2);
		}
		//
		return sumDivergence;		
	}
	//
	private double calProportionality_Scaled(Object query_repr, Set<String> S){
		double[] t_q = (double[])query_repr;
		return Math.sqrt(1d/t_q.length*calProportionality(query_repr, S));
	}
	//
	private double pContributionOfK(String doc_name_k, Object query_repr){
		double[] t_q = (double[])query_repr;
		double [] t_k = (double[])_docRepr.get(doc_name_k);
		//
		double sumDivergence = 0.0;
		for(int i=0; i<t_q.length; i++){
			sumDivergence += Math.pow(t_k[i]-t_q[i], 2);
		}
		return sumDivergence;
	}
	private double pContributionOfK_scaled(String doc_name_k, Object query_repr){
		double[] t_q = (double[])query_repr;
		return Math.sqrt(1d/t_q.length*pContributionOfK(doc_name_k, query_repr));
	}
	public String getDescription() {
		// TODO Auto-generated method stub
		return "MDP (lambda=" + _dLambda + ") -- sbkernel: " + _sbKernel.getKernelDescription();
	}
	//
	public String getString(){
		return "MDP";
	}
	public String getString(fVersion _fVersion){
		return "MDP"+_fVersion.toString();
	}
	
	//////////////////////////
	//Evaluation
	//////////////////////////
	public static final boolean DEBUG = false;
	private static DecimalFormat resultFormat = new DecimalFormat("#.####");
	private static DecimalFormat serialFormat = new DecimalFormat("00");
	//
	public void doEval(
			List<String> evalQueries, 
			HashMap<String,String> docs, 			 
			Map<String,TRECQueryAspects> stdTRECQueryAspects,
			List<Metric> lossFunctions,			
			int cutoffK,
			String output_prefix,
			String output_filename,
			fVersion [] fvArray) throws Exception {
		//
		PrintStream ps_per_SLoss  = new PrintStream(new FileOutputStream(output_prefix + output_filename + ".mdp.txt"));
		PrintStream ps_avg_SLoss = new PrintStream(new FileOutputStream(output_prefix + output_filename + ".avg.mdp.txt"));
		PrintStream ps_per_Ndeval = new PrintStream(new FileOutputStream(output_prefix + output_filename + "_ndeval.mdp.txt"));
		PrintStream ps_avg_Ndeval = new PrintStream(new FileOutputStream(output_prefix + output_filename + "_ndeval.avg.mdp.txt"));
		PrintStream err = new DevNullPrintStream(); //new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".errors.txt"));
		//------------
		//for recording the metric values
		Vector<Vector<Double>> usl_vs_rank_vector = new Vector<Vector<Double>>();
		Vector<Vector<Double>> wsl_vs_rank_vector = new Vector<Vector<Double>>();
		Vector<Vector<Double>> ndeval_vector = new Vector<Vector<Double>>();
		//
		for(int k=0; k<fvArray.length; k++){
			//for subtopic loss functions
			Vector<Double> usl_vs_rank = new Vector<Double>();
			Vector<Double> wsl_vs_rank = new Vector<Double>();
			Vector<Double> ndeval = new Vector<Double>();
			for(int j=0; j<cutoffK; j++){
				usl_vs_rank.add(0d);
				wsl_vs_rank.add(0d);
			}
			//for ndeval of 21 metric
			for(int j=0; j<21; j++){
				ndeval.add(0d);
			}
			//
			usl_vs_rank_vector.add(usl_vs_rank);
			wsl_vs_rank_vector.add(wsl_vs_rank);
			ndeval_vector.add(ndeval);
		}		
		//
		int query_serial = 0;
		TRECQuery trecQuery = null;
		for(String qNumber: evalQueries){
			// Get query relevant info
			++query_serial;
			//
			if(null != this._allTREC68Queries){
				trecQuery = _allTREC68Queries.get(qNumber);
			}else if(null != this._allTRECDivQueries){
				trecQuery = _allTRECDivQueries.get(qNumber);				
			}
			//
			TRECQueryAspects qa = stdTRECQueryAspects.get(qNumber);			
			if (DEBUG) {
				System.out.println("- Processing query '" + qNumber + "'");
				System.out.println("- Query details: " + trecQuery);
				System.out.println("- Query aspects: " + qa);
			}
			//refresh D
			clearInfoOfTopNDocs();
			//i.e., the top-n doc
			Set<String> relevant_docs = qa.getTopNDocs();
			if (DEBUG){
				System.out.println("- Evaluating with " + relevant_docs.size() + " docs");
			}			
			//add each new doc to D
			for (String aTopNdoc_name : relevant_docs) {
				if (!docs.containsKey(aTopNdoc_name))
					err.println("ERROR: '" + aTopNdoc_name + "' not found for '" + qNumber + "'");
				else {
					addATopNDoc(aTopNdoc_name);
					if (DEBUG){
						System.out.println("- [" + qNumber + "] Adding " + aTopNdoc_name);
					}					
				}
			}
			// generate the ranked list L			
			// Initialize document set
			initTonNDocsForInnerKernels();
			//
			for(int k=0; k<fvArray.length; k++){
				//k-th type of ranker
				fVersion _fVersion = fvArray[k];
				if (DEBUG){
					System.out.println("\nRunning alg: " + getString(_fVersion));
				}
				ArrayList<String> result_list = getResultList(trecQuery.getQueryContent(), cutoffK, _fVersion);
				//
				for (Metric loss : lossFunctions) {
					String loss_name = loss.getName();
					//System.out.println("Evaluating: " + loss_name);
					Object o = loss.eval(qa, result_list);
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						loss_result_str = o.toString();
					}					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + trecQuery._number + " -> " + trecQuery.getQueryContent());
					System.out.println("MDP Alg: " + getString(_fVersion));
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank_vector.set(k, VectorUtils.Sum(usl_vs_rank_vector.get(k), (double[])o));						
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), getString(_fVersion), "USL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank_vector.set(k, VectorUtils.Sum(wsl_vs_rank_vector.get(k), (double[])o));
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), getString(_fVersion), "WSL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof NDEval10Losses) {							
						ndeval_vector.set(k, VectorUtils.Sum(ndeval_vector.get(k), (double[])o));
						export(ps_per_Ndeval, "q-"+serialFormat.format(query_serial), getString(_fVersion), "NDEval10\n", (double[])o, (String [])loss.getMetricArray());
					}
					ps_per_SLoss.flush();
					ps_per_Ndeval.flush();
				}
			}			
		}
		clearInfoOfTopNDocs();
		//mean
		for(int k=0; k<fvArray.length; k++){
			usl_vs_rank_vector.set(k, VectorUtils.ScalarMultiply(usl_vs_rank_vector.get(k), 1d/evalQueries.size()));
			wsl_vs_rank_vector.set(k, VectorUtils.ScalarMultiply(wsl_vs_rank_vector.get(k), 1d/evalQueries.size()));
			ndeval_vector.set(k,  VectorUtils.ScalarMultiply(ndeval_vector.get(k), 1d/evalQueries.size()));
		}
		//
		System.out.println("==================================================");
		System.out.println("Exporting ");
		//
		for(int k=0; k<fvArray.length; k++){
			export(ps_avg_SLoss, "Mean", getString(fvArray[k])+"\t", "USL\n", usl_vs_rank_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(0).getMetricArray());
			export(ps_avg_SLoss, "Mean", getString(fvArray[k])+"\t", "WSL\n", wsl_vs_rank_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(1).getMetricArray());
			export(ps_avg_Ndeval, "Mean", getString(fvArray[k])+"\t", "NDEval10\n", ndeval_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(2).getMetricArray());
		}		
		//
		ps_per_SLoss.close();
		ps_avg_SLoss.close();
		ps_per_Ndeval.close();
		ps_avg_Ndeval.close();
		err.close();
		//------------		
	}
	
	public static void export(PrintStream ps, String query_serial, String rankerString, String lossFString, double[] v, String [] metricArray) {
		
		ps.print(query_serial + "\t" + rankerString + "\t" + lossFString);
		for (int i = 0; i < v.length; i++)
			if(metricArray[i].endsWith("\n")){
				ps.print("\t" + metricArray[i].replaceFirst("\n", ":") +resultFormat.format(v[i])+"\n");
			}else {
				ps.print("\t" + metricArray[i] + ":" +resultFormat.format(v[i]));
			}			
		ps.println();		
	}
	
	public static void export(PrintStream ps, String query_serial, String rankerString, String lossFString, Double[] v, String [] metricArray) {
		
		ps.print(query_serial + "\t" + rankerString + "\t" + lossFString);
		for (int i = 0; i < v.length; i++)
			if(metricArray[i].endsWith("\n")){
				ps.print("\t" + metricArray[i].replaceFirst("\n", ":") +resultFormat.format(v[i])+"\n");
			}else {
				ps.print("\t" + metricArray[i] + ":" +resultFormat.format(v[i]));
			}			
		ps.println();		
	}

}
