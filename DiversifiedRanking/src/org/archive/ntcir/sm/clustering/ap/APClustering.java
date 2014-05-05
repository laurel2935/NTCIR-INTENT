package org.archive.ntcir.sm.clustering.ap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm;
import org.archive.ml.clustering.ap.abs.ClusterInteger;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.MatrixPropagationAlgorithm;

public class APClustering {
	
	private AffinityPropagationAlgorithm ap = new MatrixPropagationAlgorithm();
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
    
    
	ArrayList<InteractionData> dataPointInteractions;
    
    APClustering(double lambda, int iterations, Integer convits, double preferences, String kind,
    		ArrayList<InteractionData> Interactions, boolean logDomain, boolean refine, AffinityConnectingMethod connMode){
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
        //
    }
    
    public void setParemeters() {
    	this.ap.setLambda(this.lambda);
    	this.ap.setIterations(this.iterations);
    	this.ap.setConvits(this.convits);
    	//this.ap.setSteps(this.steps);
    	this.ap.setRefine(this.refine);
    	this.ap.setConnectingMode(this.connMode);    	
        //af.addIterationListener(new ConsoleIterationListener(iterations));       
        
        for(InteractionData intData : this.dataPointInteractions){
        	this.nodeNames.add(intData.getFrom());
        	this.nodeNames.add(intData.getTo());
        }
        
        this.ap.setN(this.nodeNames.size() + 1);

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
            Integer source = Integer.valueOf(intData.getFrom());
            Integer target = Integer.valueOf(intData.getTo());
            this.ap.setSimilarityInt(source, target, val);
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
    	if (this.kind.equals("centers")) {
            Map<Integer, ClusterInteger> clusters = this.ap.doClusterAssocInt();
            //Map<Integer, Cluster<Integer>> clusters = af.doClusterAssocInt();    
            return clusters;            
        } else {
            Map<Integer, Integer> clusters = this.ap.doClusterInt(); 
            return clusters;
        }
    }
}
