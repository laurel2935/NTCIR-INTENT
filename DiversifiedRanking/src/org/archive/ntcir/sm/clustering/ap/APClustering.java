package org.archive.ntcir.sm.clustering.ap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityGraphMode;
import org.archive.ml.clustering.ap.abs.ClusterInteger;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.MatrixPropagationAlgorithm;
import org.archive.util.format.StandardFormat;


public class APClustering {
	
	private static final boolean debug = true;
	
	private MatrixPropagationAlgorithm ap = new MatrixPropagationAlgorithm();
    //lambda for damping
    private double lambda;
    //maximum number of iterations to be performed
    private int iterations;
    //given preference
    private double preferences;
    /*
     * algorithm will stop after the n-th{i.e., convits} iteration without any change in centers (exemplars).
     * In this situation, we say that the algorithm converged.
     * If the maximum number of interactions is reached before the stop criterion is satisfied,
     * then the algorithm did not converge. The current clusters can still be retrieved in this situation.
     * */
    private Integer convits;
    //set of node identifier, i.e., names
    private Collection<String> nodeNames = new HashSet<String>();
    private String kind;
    private boolean takeLog;
    private boolean refine;
    //?
    //private Integer steps = null;
    private AffinityConnectingMethod connMode;
    private AffinityGraphMode graphMode;
    
    
	ArrayList<InteractionData> dataPointInteractions;
    
    APClustering(double lambda, int iterations, Integer convits, double preferences, String kind,
    		ArrayList<InteractionData> Interactions, boolean logDomain, boolean refine, AffinityConnectingMethod connMode, AffinityGraphMode graphMode){
    	this.lambda = lambda;
        this.iterations = iterations;
        this.preferences = preferences;
        this.kind = kind;
        this.convits = convits;
        this.dataPointInteractions = Interactions;
        this.takeLog = logDomain;
        this.refine = refine;
        //this.steps = steps;
        this.connMode = connMode;
        this.graphMode = graphMode;
        //
    }
    
    public void setParemeters() {
    	this.ap.setLambda(this.lambda);
    	this.ap.setIterations(this.iterations);
    	this.ap.setConvits(this.convits);
    	//this.ap.setSteps(this.steps);
    	this.ap.setRefine(this.refine);
    	this.ap.setConnectingMode(this.connMode);   
    	this.ap.setGraphMode(this.graphMode);
        //af.addIterationListener(new ConsoleIterationListener(iterations));       
        
        for(InteractionData intData : this.dataPointInteractions){
        	this.nodeNames.add(intData.getFrom());
        	this.nodeNames.add(intData.getTo());
        }
        
        this.ap.setN(this.nodeNames.size());

        this.ap.init();
        
        for (InteractionData intData : this.dataPointInteractions) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());
            Double val;
            if (this.takeLog) {
                if (intData.getSim() > 0) {
                    val = Math.log(intData.getSim());
                } else {
                    val = Double.valueOf(0);
                }
            } else {
                val = intData.getSim();
            }            
            this.ap.setSimilarity(intData.getFrom(), intData.getTo(), val);
            //af.setSimilarityInt(target, source, val);
            //af.setSimilarityInt(Integer.valueOf(intData.getFrom()), Integer.valueOf(intData.getTo()), val);
        }
        Double pref;
        if (this.takeLog) {
            if (this.preferences > 0) {
                pref = Math.log(this.preferences);
            } else {
                pref = Double.valueOf(0);
            }
        } else {
            pref = this.preferences;
        }
        System.out.println("pref: " + pref);
        this.ap.setConstPreferences(pref);
    }
    
    public Object run() {
    	 if(debug){
         	System.out.println(this.ap.S.toString());
         }
    	//
    	if (this.kind.equals("centers")) {
            Map<Integer, ClusterInteger> clusters = this.ap.doClusterAssocInt();
            //Map<Integer, Cluster<Integer>> clusters = af.doClusterAssocInt();    
            return clusters;            
        } else {
            Map<Integer, Integer> clusters = this.ap.doClusterInt(); 
            if(debug){
            	System.out.println("AP results: ");
            	for(Entry<Integer, Integer> entry: clusters.entrySet()){
            		System.out.println(entry.getKey()+"  ->  "+entry.getValue());
            	}
            }
            return clusters;
        }
    }
    
    public static double getSimilarity(ArrayList<Double> aList, ArrayList<Double> bList){
    	double eucDistance = Math.sqrt(Math.pow(aList.get(0)-bList.get(0), 2)
    			+ Math.pow(aList.get(1)-bList.get(1), 2));
    	//
    	return -eucDistance;
    }
    
    public static double getMedian(ArrayList<Double> vList){
    	Collections.sort(vList);
    	if(vList.size() % 2 == 0){
    		return (vList.get(vList.size()/2 - 1)+vList.get(vList.size()/2))/2.0;
    	}else{    		
    		return vList.get(vList.size()/2);
    	}
    }
    
    public static void test(){
    	//4 data points
    	ArrayList<ArrayList<Double>> datapointList = new ArrayList<ArrayList<Double>>();
    	ArrayList<Double> dataPoint_1 = new ArrayList<Double>();
    	dataPoint_1.add(-2.3);
    	dataPoint_1.add(3.7);
    	datapointList.add(dataPoint_1);
    	
    	ArrayList<Double> dataPoint_2 = new ArrayList<Double>();
    	dataPoint_2.add(-1.5);
    	dataPoint_2.add(1.8);
    	datapointList.add(dataPoint_2);
    	
    	ArrayList<Double> dataPoint_3 = new ArrayList<Double>();
    	dataPoint_3.add(2.5);
    	dataPoint_3.add(1.8);
    	datapointList.add(dataPoint_3);
    	
    	ArrayList<Double> dataPoint_4 = new ArrayList<Double>();
    	dataPoint_4.add(4.0);
    	dataPoint_4.add(1.6);
    	datapointList.add(dataPoint_4);
    	//
    	ArrayList<Double> vList = new ArrayList<Double>();
    	ArrayList<InteractionData> dataPointInteractions = new ArrayList<InteractionData>();
    	for(int i=0; i<datapointList.size()-1; i++){
    		for(int j=i+1; j<datapointList.size(); j++){
    			double v = getSimilarity(datapointList.get(i), datapointList.get(j));
    			InteractionData interData = new InteractionData(StandardFormat.serialFormat(i, "00"), 
    					StandardFormat.serialFormat(j, "00"), 
    					v);
    			dataPointInteractions.add(interData);
    			vList.add(v);
    		}
    	}    	
    	//
    	double lambda = 0.5;
    	int iterations = 50000000;
    	int convits = 20;
    	String kind = "";
    	boolean logDomain = false;
    	boolean refine = false;
    	AffinityConnectingMethod connMode = AffinityConnectingMethod.ORIGINAL;
    	AffinityGraphMode graphMode = AffinityGraphMode.UNDIRECTED;
    	//double preferences = getMedian(vList);
    	double preferences = -4.0;
    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, kind,
    			dataPointInteractions, logDomain, refine, connMode, graphMode);
    	//
    	apClustering.setParemeters();
    	apClustering.run();
    }
    
    //
    public static void main(String []args){
    	//
    	APClustering.test();
    	
    }
}
