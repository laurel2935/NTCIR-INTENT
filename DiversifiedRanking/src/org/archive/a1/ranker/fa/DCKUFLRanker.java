package org.archive.a1.ranker.fa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.Mat;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.sm.pro.DCKUFL;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class DCKUFLRanker extends ResultRanker{
	//as this test set contains explicit subtopics
	private Map<String,TRECDivQuery> _allTRECDivQueries = null;
	//kernel, under which each query, subtopic, document is represented
	public Kernel _kernel;
		
	// Constructor
	public DCKUFLRanker(HashMap<String, String> docs, Kernel kernel, Map<String,TRECDivQuery> allTRECDivQueries) { 
		super(docs);				
		this._kernel = kernel;	
		this._allTRECDivQueries = allTRECDivQueries;	
	}
	
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	//refresh each time for a query
	//_docOrig, i.e., the top-n set of a query
	public void clearInfoOfTopNDocs() {
		//_docRepr.clear();		
		_docs_topn.clear();
		_kernel.clearInfoOfTopNDocs();	
	}
	//called when a new query come
	public void initTonNDocsForInnerKernels() {
		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_kernel.initTonNDocs(_docs_topn); // LDA should maintain keys for mapping later		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		//for (String doc : _docs_topn) {
			//Object repr = _kernel.getObjectRepresentation(doc);
			//in the currrent case, as only one kernel is used, the kernel itself owns the buffer
			//_docRepr.put(doc, repr);			
		//}
	}	
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	//
	private ArrayList<InteractionData> getReleMatrix(TRECDivQuery trecDivQuery){
		ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();		
		//subtopic ¡Á top-n docs
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		String [] topNDocs = _docs_topn.toArray(new String[0]);
		for(TRECSubtopic trecSubtopic: trecSubtopicList){	
			Object subtopicRepr = _kernel.getNoncachedObjectRepresentation(trecSubtopic.getContent());
			for(String docName: topNDocs){
				Object docRepr = _kernel.getObjectRepresentation(docName);
				//
				releMatrix.add(new InteractionData(trecSubtopic._sNumber, docName, _kernel.sim(subtopicRepr, docRepr)));
			}
		}
		//
		return releMatrix;		
	}
	//
	private ArrayList<InteractionData> getSubtopicSimMatrix(TRECDivQuery trecDivQuery){
		ArrayList<InteractionData> simMatrix = new ArrayList<InteractionData>();
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		//
		for(int i=0; i<trecSubtopicList.size()-1; i++){
			TRECSubtopic iSubtopic = trecSubtopicList.get(i);
			Object iRepr = _kernel.getNoncachedObjectRepresentation(iSubtopic.getContent());
			for(int j=i+1; j<trecSubtopicList.size(); j++){
				TRECSubtopic jSubtopic = trecSubtopicList.get(j);
				Object jRepr = _kernel.getNoncachedObjectRepresentation(jSubtopic.getContent());
				//
				simMatrix.add(new InteractionData(iSubtopic._sNumber, jSubtopic._sNumber, _kernel.sim(iRepr, jRepr)));
			}
		}
		//
		return simMatrix;	
	}
	//
	private ArrayList<Double> getCapacityList(TRECDivQuery trecDivQuery, int topK){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		if(topK%subtopicNumber == 0){
			double cap = topK/subtopicNumber;
			return Mat.getUniformList(cap, subtopicNumber);
		}else{
			double cap = topK/subtopicNumber+1;
			return Mat.getUniformList(cap, subtopicNumber);
		}		
	}
	//
	private ArrayList<Double> getPopularityList(TRECDivQuery trecDivQuery){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		return Mat.getUniformList(1.0/subtopicNumber, subtopicNumber);		
	}
	//
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		//
		ArrayList<InteractionData> releMatrix = getReleMatrix(trecDivQuery);
		ArrayList<InteractionData> subSimMatrix = getSubtopicSimMatrix(trecDivQuery);
		ArrayList<Double> capList = getCapacityList(trecDivQuery, size);
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		//
    	double lambda = 0.5;
    	int iterationTimes = 5000;
    	int noChangeIterSpan = 10; 
    	int preK = (int)Mat.sum(capList); 
    	//
    	DCKUFL dckufl = new DCKUFL(lambda, iterationTimes, noChangeIterSpan, preK, releMatrix, subSimMatrix, capList, popList);
    	dckufl.run();
    	ArrayList<String> facilityList = dckufl.getSelectedFacilities();
    	//
    	ArrayList<StrDouble> objList = new ArrayList<StrDouble>();
    	Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
    	for(String docName: facilityList){
    		objList.add(new StrDouble(docName, _kernel.sim(queryRepr, _kernel.getObjectRepresentation(docName))));
    	}
    	Collections.sort(objList, new PairComparatorBySecond_Desc<String, Double>());
    	
    	ArrayList<String> resultList = new ArrayList<String>();
    	for(int i=0; i<size; i++){
    		resultList.add(objList.get(i).getFirst());
    	}
    	
    	return resultList;		
	}
	//
	public String getString(){
		return "DCKUFL";
	}
	//
	public String getDescription() {
		// TODO Auto-generated method stub
		return "DCKUFL - sbkernel: " + _kernel.getKernelDescription();
	}
	
}
