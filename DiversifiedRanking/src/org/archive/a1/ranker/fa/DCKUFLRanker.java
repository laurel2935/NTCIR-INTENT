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
	
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	private int _noChangeIterSpan = 10; 
	
	//kernel, under which each query, subtopic, document is represented
	public Kernel _kernel;
		
	// Constructor
	public DCKUFLRanker(HashMap<String, String> docs, Kernel kernel, double lambda, int iterationTimes, int noChangeIterSpan) { 
		super(docs);				
		this._kernel = kernel;	
		this._lambda = lambda;
		this._iterationTimes = iterationTimes;
		this._noChangeIterSpan = noChangeIterSpan;
		//
		this._indexOfGetResultMethod = 1;
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
		//The similarity kernel may need to do pre-processing (e.g., LDA training)
		_kernel.initTonNDocs(_docs_topn);
	}	
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	//relevance between subtopics and documents
	private ArrayList<InteractionData> getReleMatrix(TRECDivQuery trecDivQuery){
		//
		initTonNDocsForInnerKernels();
		
		ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();		
		//subtopic <-> docs
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		String [] topNDocNames = _docs_topn.toArray(new String[0]);
		for(TRECSubtopic trecSubtopic: trecSubtopicList){	
			Object subtopicRepr = _kernel.getNoncachedObjectRepresentation(trecSubtopic.getContent());
			for(String docName: topNDocNames){
				Object docRepr = _kernel.getObjectRepresentation(docName);
				//
				releMatrix.add(new InteractionData(trecSubtopic._sNumber, docName, _kernel.sim(subtopicRepr, docRepr)));
			}
		}
		//
		return releMatrix;		
	}
	//similarity among subtopics
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
	//equal size of capacity of each subtopic
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
		return Mat.getUniformList(1.0d/subtopicNumber, subtopicNumber);		
	}
	//
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		//
		ArrayList<InteractionData> releMatrix = getReleMatrix(trecDivQuery);
		ArrayList<InteractionData> subSimMatrix = getSubtopicSimMatrix(trecDivQuery);
		ArrayList<Double> capList = getCapacityList(trecDivQuery, size);
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		    	
    	int preK = (int)Mat.sum(capList); 
    	
    	DCKUFL dckufl = new DCKUFL(_lambda, _iterationTimes, _noChangeIterSpan, preK, releMatrix, subSimMatrix, capList, popList);
    	dckufl.run();
    	ArrayList<String> facilityList = dckufl.getSelectedFacilities();
    	//(1)final ranking by similarity between query and document
    	ArrayList<StrDouble> objList = new ArrayList<StrDouble>();
    	Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
    	for(String docName: facilityList){
    		objList.add(new StrDouble(docName, _kernel.sim(queryRepr, _kernel.getObjectRepresentation(docName))));
    	}
    	//(2)??? similarity between subtopic vector and document
    	
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
