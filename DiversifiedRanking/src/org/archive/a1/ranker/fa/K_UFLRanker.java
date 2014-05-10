package org.archive.a1.ranker.fa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.K_UFL;
import org.archive.ml.ufl.K_UFL.UFLMode;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class K_UFLRanker extends ResultRanker{
	//as this test set contains explicit subtopics
	private Map<String,TRECDivQuery> _allTRECDivQueries = null;
	//kernel, under which each query, subtopic, document is represented
	public Kernel _kernel;
	
	// Constructor
	public K_UFLRanker(HashMap<String, String> docs, Kernel kernel, Map<String,TRECDivQuery> allTRECDivQueries) { 
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
		
		String [] topNDocs = _docs_topn.toArray(new String[0]);
		for(int i=0; i<topNDocs.length-1; i++){
			String iDocName = topNDocs[i]; 
			Object iDocRepr = _kernel.getObjectRepresentation(iDocName);
			for(int j=i+1; j<topNDocs.length; j++){
				String jDocName = topNDocs[j];
				Object jDocRepr = _kernel.getObjectRepresentation(jDocName);
				//
				releMatrix.add(new InteractionData(iDocName, jDocName, _kernel.sim(iDocRepr, jDocRepr)));				
			}
		}
		
		return releMatrix;		
	}
	
	private ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	
	public double getMedian(ArrayList<Double> vList){
		Collections.sort(vList);
		//
		if(vList.size() % 2 == 0){
			return (vList.get(vList.size()/2)+vList.get(vList.size()/2 - 1))/2.0;
		}else{
			return vList.get(vList.size()/2);
		}
	}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		//
		ArrayList<InteractionData> releMatrix = getReleMatrix(trecDivQuery);
		ArrayList<InteractionData> costMatrix = getCostMatrix(releMatrix);		
		//
    	double lambda = 0.5;
    	int iterationTimes = 5000;
    	int noChangeIterSpan = 10; 
    	int preK = size; 
    	ArrayList<Double> vList = new ArrayList<Double>();
    	for(InteractionData itrData: costMatrix){
    		vList.add(itrData.getSim());
    	}
    	double costPreferences = getMedian(vList);
    	//
    	K_UFL kUFL = new K_UFL(lambda, iterationTimes, noChangeIterSpan, costPreferences, preK, UFLMode.C_Same_F, costMatrix);
    	//
    	Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
    	ArrayList<Double> fList = new ArrayList<Double>();
    	for(int j=0; j<_docs_topn.size(); j++){
    		String docName = kUFL.getFacilityName(j);
    		Object jDocRepr = _kernel.getObjectRepresentation(docName);
    		fList.add(0-_kernel.sim(queryRepr, jDocRepr));
    	}
    	kUFL.setFacilityCost(fList);
    	
    	kUFL.run();
    	
    	ArrayList<String> facilityList = kUFL.getSelectedFacilities();
    	//
    	ArrayList<StrDouble> objList = new ArrayList<StrDouble>();
    	
    	for(String docName: facilityList){
    		objList.add(new StrDouble(docName, 0-fList.get(kUFL.getFacilityID(docName))));
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
		return "K_UFL";
	}
	//
	public String getDescription() {
		// TODO Auto-generated method stub
		return "K_UFL - sbkernel: " + _kernel.getKernelDescription();
	}

}
