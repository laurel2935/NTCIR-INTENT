package org.archive.ntcir.sm.pro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.archive.ml.clustering.ap.abs.ConvitsVector;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;

public class DiversifiedDRViaKUFL {
	//basic parameters//
	private static final double INF = 1000000000.0;
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;
    //given preference
    //private double constPreferences;
    //as the cost matrix takes the negative value of similarity matrix, thus ...
    //private boolean _logDomain;
    private ArrayList<InteractionData> _subtopicDocRelevanceList;
    //private ArrayList<Double> _capdqRelevanceList;
    
    //set of node identifier, i.e., names
    private Collection<String> _subtopicNodeSet = new HashSet<String>();
    private Collection<String> _docNodeSet = new HashSet<String>(); 
    protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
	
	///////////////////////
	// the corresponding index in the paper i,j  will be essentially i-1 j-1 in the program!
	///////////////////////
	
	//number of customers	0<=i<N
	private int _N;
	//number of potential facilities	0<=j<M
	private int _M;
	//relevance between subtopic and doc,
	//i.e., the negative c_ij: the cost of assigning a customer i to facility j
	private DoubleMatrix2D _R;
	//Y_j the cost of opening the facility Y_j
	//one-row object
	//private DoubleMatrix2D _Y;
	
	//N¡ÁM
	private DoubleMatrix2D _Eta;
	private DoubleMatrix2D _oldEta;
	
	//1¡ÁM
	private DoubleMatrix2D _V;
	private DoubleMatrix2D _oldV;
	
	//N¡ÁM
	private DoubleMatrix2D _Alpha;
	private DoubleMatrix2D _oldAlpha;
	
	//(M+1)¡ÁM the row corresponds to the state of z_j, the column is the j-th column
	private DoubleMatrix2D _A;
	private DoubleMatrix2D _oldA;
	private DoubleMatrix2D _B;
	private DoubleMatrix2D _oldB;
	
	//one-row object
	private DoubleMatrix2D _Gama;
	private DoubleMatrix2D _oldGama;
	
	//one-column object
	private DoubleMatrix2D _H;
	private DoubleMatrix2D _oldH;
	
	//exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D IX = null;    
    private IntegerMatrix1D IY = null;
	
    //the number of exemplar
    private int clustersNumber = 0;
	
    //pay attention to the positive or negative value of dataPointInteractions &&¡¡fCostList
    DiversifiedDRViaKUFL(double lambda, int iterationTimes, Integer noChangeIterSpan, double preferences, 
    		ArrayList<InteractionData> subtopicDocRelevanceList, ArrayList<Double> dqRelevanceList){
    	//1
    	//dataPointSimilarities, for cost, e.g., c_ij, it would be the negative value of each similarity
    	//relevanceList, for facility f_j, it would be the negative value of each one
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;
    	this._subtopicDocRelevanceList = subtopicDocRelevanceList;
    	//this._dqRelevanceList = dqRelevanceList;
    	//this._logDomain = logDomain;
    	
    	//2
    	this.ini();
    }
    
    private void ini(){    	    	
    	this._subtopicNodeSet = new HashSet<String>();
    	this._docNodeSet = new HashSet<String>();
    	//order : from: subtopic name to: doc name
    	for(InteractionData intData : this._subtopicDocRelevanceList){
        	this._subtopicNodeSet.add(intData.getFrom());
        	this._docNodeSet.add(intData.getTo());
        }
        this._N = this._subtopicNodeSet.size();
        this._M = this._docNodeSet.size();
        
        //cost matrix c_ij
        this._R = new DoubleMatrix2D(this._N, this._M, 0);
        
        for (InteractionData intData : this._subtopicDocRelevanceList) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());           
            Integer source = Integer.valueOf(intData.getFrom());
            Integer target = Integer.valueOf(intData.getTo());
            //because the relevance value is directly used in the derivation
            double r = intData.getSim();
            this._R.set(source, target, r);            
        } 
        
        //
        this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        this._H = new DoubleMatrix2D(this._N, 1, 0);
        this._A = new DoubleMatrix2D();
        this._B = new DoubleMatrix2D();
    }	
	
	public void computeBeliefs(){
		DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta);
        //the indexes of potential exemplars
        IX = EX.diag().findG(0);
        //
        DoubleMatrix2D EY;
        EY = this._V.plus(this._Gama);
        IY = EY.diag().findG(0);
	}
	
	public IntegerMatrix1D getSelectedDocs(){
		return this.IY;
	}	
	
	
	//// Eta ////
	private void copyEta(){
		this._oldEta = this._Eta.copy();
	}
	//
	private void computeEta(){
		
	}
	//
	private void updateEta(){
		this._Eta = this._Eta.mul(1-getLambda()).plus(this._oldEta.mul(getLambda()));
	}
	
	//// V ////
	private void copyV(){
		this._oldV = this._V.copy();
	}
	//
	private void computeV(){
				
	}
	//
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	
	//// Alpha ////
	private void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
	}
	private void computeAlpha(){
		
	}
	//
	private void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	
	////  (a,b) update////
	private void copyAB(){
		this._oldA = this._A.copy();
		this._oldB = this._B.copy();
	}	
	private void computeAB(){
		
	}
	//
	private void updateAB(){
		this._A = this._A.mul(1-getLambda()).plus(this._oldA.mul(getLambda()));
		this._B = this._B.mul(1-getLambda()).plus(this._oldB.mul(getLambda()));
	}
	
	//// Gama ////
	private void copyGama(){
		this._oldGama = this._Gama.copy();
	}
	//
	private void computeGama(){
		
	}
	//
	private void updateGama(){
		this._Gama = this._Gama.mul(1-getLambda()).plus(this._oldGama.mul(getLambda()));
	}
		
	//// H ////
	private void copyH(){
		this._oldH = this._H.copy();
	}
	private void computeH(){}
	private void updateH(){
		this._H = this._H.mul(1-getLambda()).plus(this._oldH.mul(getLambda()));
	}
	
	
	protected void computeExemplars() {
        DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta);
        //the indexes of potential exemplars
        this.IX = EX.diag().findG(0);
        this.clustersNumber = this.IX.size();
    }
	
	
	public void run(){
		int itrTimes = getIterationTimes();
		initConvergence();
		
		for(int itr=1; itr<=itrTimes; itr++){
			//
			this.copyEta();
			this.computeEta();
			this.updateEta();
			
			//
			this.copyAlpha();
			this.computeAlpha();
			this.updateAlpha();
			
			//
			this.copyV();
			this.computeV();
			this.updateV();
			
			//
			this.copyAB();
			this.computeAB();
			this.updateAB();
			
			//
			this.copyGama();
			this.computeGama();
			this.updateGama();
			
			//
			this.copyH();
			this.computeH();
			this.updateH();
			
			//
			computeExemplars();
			
			calculateCovergence();
			
			if(!checkConvergence()){
				break;
			}
		}
		
		computeBeliefs();
	}
	
	/**
     * initialize the indicator of convergence vectors
     * **/
    protected void initConvergence() {
        //System.out.println("S: " + S.toString());
        if (this._noChangeIterSpan != null) {
            for (int i = 0; i < this._N; i++) {
                ConvitsVector vec = new ConvitsVector(this._noChangeIterSpan.intValue(), Integer.valueOf(i));
                vec.init();
                this.convitsVectors.put(Integer.valueOf(i), vec);
            }
        }
    }
    //
    protected void calculateCovergence() {
        if (this._noChangeIterSpan != null) {
            Vector<Integer> c = IX.getVector();
            for (int i = 0; i < this._N; i++) {
                Integer ex = Integer.valueOf(i);
                //after each iteration, examine whether each node is an exemplar,
                //then check the sequential true or false value of each node to determine convergence!
                if (c.contains(ex)) {
                	this.convitsVectors.get(ex).addCovits(true);
                } else {
                	this.convitsVectors.get(ex).addCovits(false);
                }
            }
        }
    }
    /**
     * @return true: notConverged / false:converged, essentially, whether there is no change given a predefined span of iteration
     * **/
    protected boolean checkConvergence() {
    	//no cluster generated!
        if (getClustersNumber() == 0) {
            return true;
        }
        //
        if (this._noChangeIterSpan == null) {
            return true;
        } else {
            for (ConvitsVector vec : convitsVectors.values()) {
                if (vec.checkConvits() == false) {
                    return true;
                }
            }
        }
        return false;
    }
	
	
	
	
	//
	public double getLambda(){
		return this._lambda;
	}
	//
	public int getClustersNumber(){
		return this.clustersNumber;
	}
	//
	public int getIterationTimes(){
		return this._iterationTimes;
	}

}
