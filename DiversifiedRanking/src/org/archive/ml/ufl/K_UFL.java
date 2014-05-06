package org.archive.ml.ufl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.archive.ml.clustering.ap.abs.ConvitsVector;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityGraphMode;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;
import org.archive.ntcir.sm.clustering.ap.APClustering;
import org.archive.util.format.StandardFormat;

public class K_UFL {
	
	private static final boolean debug = true;
	
	public enum UFLMode {C_Same_F, C_Differ_F}
	
	//basic parameters//
	//private static final double INF = 1000000000.0;
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;
    //given preference
    private double _costPreferences;
    //as the cost matrix takes the negative value of similarity matrix, thus ...
    //private boolean _logDomain;
    private ArrayList<InteractionData> _dataPointSimilarities;
    private ArrayList<Double> _dqRelevanceList;
    
    //set of node identifier, i.e., names
    private Collection<String> _cNodeNames;
    private Collection<String> _fNodeNames;
    protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
    
    private UFLMode _uflMode;
	
    //predefined k
    Integer G_M1_zM;
    
	///////////////////////
	// the corresponding index in the paper i,j  will be essentially i-1 j-1 in the program!
	///////////////////////
	
	//number of customers	0<=i<N
	private int _N;
	//number of potential facilities	0<=j<M
	private int _M;
	//C_ij: the cost of assigning a customer i to facility j
	private DoubleMatrix2D _C;
	//Y_j the cost of opening the facility Y_j
	//one-row object
	private DoubleMatrix2D _Y;
	
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
	//private DoubleMatrix2D _oldA;
	private DoubleMatrix2D _B;
	//private DoubleMatrix2D _oldB;
	
	//one-row object
	private DoubleMatrix2D _Gama;
	private DoubleMatrix2D _oldGama;
	
	//exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D IX = null;    
    private IntegerMatrix1D IY = null;
	
    //the number of exemplar
    private int clustersNumber = 0;
	
    //pay attention to the positive or negative value of dataPointInteractions &&¡¡fCostList
    K_UFL(double lambda, int iterationTimes, Integer noChangeIterSpan, double preferences, Integer preK, UFLMode uflMode, 
    		ArrayList<InteractionData> dataPointSimilarities, ArrayList<Double> dqRelevanceList){
    	//1
    	//dataPointSimilarities, for cost, e.g., c_ij, it would be the negative value of each similarity
    	//relevanceList, for facility f_j, it would be the negative value of each one
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;
    	this._costPreferences = preferences;
    	this._dataPointSimilarities = dataPointSimilarities;
    	this._dqRelevanceList = dqRelevanceList;
    	this.G_M1_zM = preK;
    	this._uflMode = uflMode;
    	//this._logDomain = logDomain;
    	
    	//2
    	this.ini();
    }
    
    private void ini(){    	
    	this._cNodeNames = new HashSet<String>();
    	this._fNodeNames = new HashSet<String>();
    	for(InteractionData intData : this._dataPointSimilarities){
        	this._cNodeNames.add(intData.getFrom());
        	this._fNodeNames.add(intData.getTo());
        }
    	if(UFLMode.C_Same_F == _uflMode){
    		HashSet<String> nodeSet = new HashSet<String>();
    		nodeSet.addAll(_cNodeNames);
    		nodeSet.addAll(_fNodeNames);
    		this._N = nodeSet.size();
    	    this._M = nodeSet.size();
    	}else{
    		this._N = this._cNodeNames.size();
    		this._M = this._fNodeNames.size();
    	}       
        
        //cost matrix c_ij
        this._C = new DoubleMatrix2D(this._N, this._M, 0);
        
        for (InteractionData intData : this._dataPointSimilarities) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());          
            double c_ij = -intData.getSim();            
            setCost(intData.getFrom(), intData.getTo(), c_ij);
        }        
        if(UFLMode.C_Same_F == _uflMode){
        	System.out.println("pref: " + _costPreferences);        
            for (int i = 0; i < this._N; i++) {
            	double c_ii = -_costPreferences;
            	this._C.set(i, i, c_ii);
            }
        }
        //facility cost f_j
        this._Y = new DoubleMatrix2D(1, this._M, 0);
        for(int j=0; j<this._M; j++){
        	double f_j = this._dqRelevanceList.get(j);
        	this._Y.set(0, j, f_j);
        }
        
        //
        this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        
        if(debug){
        	System.out.println("Cost matrix:");
        	System.out.println(_C.toString());
        	System.out.println("Y matrix:");
        	System.out.println(_Y.toString());
        }
    }	
    
    private Integer _customerID = 0;
    protected Map<String, Integer> _customerIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _customerIDRevMapper = new TreeMap<Integer, String>();
    
    private Integer _facilityID = 0;
    protected Map<String, Integer> _facilityIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _facilityIDRevMapper = new TreeMap<Integer, String>();
    
    protected Integer getCustomerID(String cName) {
        if (_customerIDMapper.containsKey(cName)) {
            return _customerIDMapper.get(cName);
        } else {
            Integer id = _customerID;
            _customerIDMapper.put(cName, id);
            _customerIDRevMapper.put(id, cName);
            _customerID++;
            return id;
        }
    }
    
    protected Integer getFacilityID(String fName){
    	if(_facilityIDMapper.containsKey(fName)){
    		return _facilityIDMapper.get(fName);
    	}else{
    		Integer id = _facilityID;
    		_facilityIDMapper.put(fName, id);
    		_facilityIDRevMapper.put(id, fName);
    		_facilityID++;
    		return id;
    	}
    }
    
    public void setCost(final String from, final String to, final Double sim) {

        Integer cID = getCustomerID(from);
        Integer fID = getFacilityID(to);
        if (UFLMode.C_Differ_F == _uflMode) {
        	_C.set(cID, fID, sim.doubleValue());
        } else {
        	_C.set(cID, fID, sim.doubleValue());
        	_C.set(fID, cID, sim.doubleValue());
        }
    }
	
	public void computeBeliefs(){
		DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta).minus(this._C);
        //the indexes of potential exemplars
        IX = EX.diag().findG(0);        
        //
        DoubleMatrix2D EY;
        EY = this._V.minus(this._Y).plus(this._Gama);
        IY = EY.diag().findG(0);
        if(debug){
        	System.out.println("Selected Facilities:");
        	System.out.println(IY.toString());
        }
	}
	
	public IntegerMatrix1D getSelectedDocs(){
		return this.IY;
	}	
	
	
	//// Eta ////
	private void copyEta(){
		this._oldEta = this._Eta.copy();
		if(debug){
        	System.out.println("old Eta:");
        	System.out.println(_oldEta.toString());
        }
	}
	/*-max_{k uneq j}[alpha_{ik} - c_{ik}]*/
	private void computeEta(){
		//alpha-c
		DoubleMatrix2D Alpha_minus_C;
		//maximum element of each row of Alpha_minus_C
		DoubleMatrix2D rMax;
		//for 2nd-maximum element of each row used for the exact maximum element
		DoubleMatrix2D rMax2;
		
		Alpha_minus_C = this._Alpha.minus(this._C);
		rMax = Alpha_minus_C.maxr();		
		for(int i=0; i<this._N; i++){
			Alpha_minus_C.set(i, (int)rMax.get(i, 0), Double.NEGATIVE_INFINITY);
		}
		rMax2 = Alpha_minus_C.maxr();
		
		double [] maxElements = new double[this._N];
		for(int i=0; i<this._N; i++){
			maxElements[i] = rMax.get(i, 1);
		}
		DoubleMatrix2D maxMatrix = new DoubleMatrix2D(this._M, maxElements);
		
		DoubleMatrix2D zeroMatrix = new DoubleMatrix2D(this._N, this._M, 0);
		this._Eta = zeroMatrix.minus(maxMatrix);
		for(int i=0; i<this._N; i++){
			this._Eta.set(i, (int)rMax.get(i, 0), 0-rMax2.get(i, 1));
		}		
	}
	//
	private void updateEta(){
		this._Eta = this._Eta.mul(1-getLambda()).plus(this._oldEta.mul(getLambda()));
	}
	
	//// V ////
	private void copyV(){
		this._oldV = this._V.copy();
		if(debug){
        	System.out.println("old V:");
        	System.out.println(_oldV.toString());
        }
	}
	//
	private void computeV(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		this._V = maxZero.sumEachColumn();		
	}
	//
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	
	//// Alpha ////
	private void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
		if(debug){
        	System.out.println("old Alpha:");
        	System.out.println(_oldAlpha.toString());
        }
	}
	private void computeAlpha(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		DoubleMatrix2D columnSum = maxZero.sumEachColumn();
		//
		//DoubleMatrix2D
		DoubleMatrix2D Gama_minus_Y = this._Gama.minus(this._Y);
		//the value at ij should not be added
		DoubleMatrix2D sameColumnVector = Gama_minus_Y.plus(columnSum);
		double [] sameColumnArray = new double [this._M];
		for(int j=0; j<this._M; j++){
			sameColumnArray[j] = sameColumnVector.get(0, j);
		}
		DoubleMatrix2D transposedTarget = new DoubleMatrix2D(this._N, sameColumnArray);
		DoubleMatrix2D target = transposedTarget.transpose();
		DoubleMatrix2D rightMatrix = target.minus(maxZero);
		this._Alpha = rightMatrix.min(0);		
	}
	//
	private void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	
	////  (a,b) update////
	/*
	private void copyAB(){
		this._oldA = this._A.copy();
		this._oldB = this._B.copy();
	}
	*/
	private void updateAB(){
		this._A = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    this._B = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    //a-update
	    //a0(z0)=0
	    this._A.set(0, 0, 0);
	    for(int j=1; j<=this._M; j++){
	    	//M+1 possible states for z_j
	    	for(int zj=0; zj<=this._M; zj++){
	    		if(0 == zj){
	    			this._A.set(zj, j, this._A.get(zj, j-1));
	    		}else{
	    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1),
		    				this._A.get(zj-1, j-1)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }
	    //b-update
	    //bM(zM)=G_M1_zM
	    for(int zM=0; zM<=this._M; zM++){
	    	this._B.set(zM, this._M, this.G_M1_zM);
	    }
	    for(int j=this._M; j>=1; j--){
	    	for(int zj_minus_1=0; zj_minus_1<=this._M; zj_minus_1++){
	    		//
	    		if(this._M == zj_minus_1){
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
		    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }	
	    //
	    if(debug){
        	System.out.println("AB update:");
        	System.out.println(_A.toString());
        	System.out.println(_B.toString());
        }
	}
	//
	/*
	private void updateAB(){
		this._A = this._A.mul(1-getLambda()).plus(this._oldA.mul(getLambda()));
		this._B = this._B.mul(1-getLambda()).plus(this._oldB.mul(getLambda()));
	}
	*/
	
	//// Gama ////
	private void copyGama(){
		this._oldGama = this._Gama.copy();
		if(debug){
        	System.out.println("old Gama:");
        	System.out.println(_oldGama.toString());        	
        }
	}
	//
	private void computeGama(){
		for(int paperJ=1; paperJ<=this._M; paperJ++){
			//1st factor
			double maxMinuend = Double.NEGATIVE_INFINITY;
			//2nd factor
			double  maxSubtrahend = Double.NEGATIVE_INFINITY; 
			for(int z=0; z<=this._M; z++){
				if(z > 0){
					double minuendSum = this._A.get(z-1, paperJ-1)+this._B.get(z, paperJ);
					if(minuendSum > maxMinuend){
						maxMinuend = minuendSum;
					}
				}
				//
				double subtrahendSum = this._A.get(z, paperJ-1)+this._B.get(z, paperJ);
				if(subtrahendSum > maxSubtrahend){
					maxSubtrahend = subtrahendSum;
				}				
			}
			//
			this._Gama.set(0, paperJ-1, maxMinuend-maxSubtrahend);
		}
	}
	//
	private void updateGama(){
		this._Gama = this._Gama.mul(1-getLambda()).plus(this._oldGama.mul(getLambda()));
	}
	
	
	
	protected void computeExemplars() {
        DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta).minus(this._C);
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
			//this.copyAB();
			//this.computeAB();
			this.updateAB();
			
			//
			this.copyGama();
			this.computeGama();
			this.updateGama();
			
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
	//
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
    			double v = APClustering.getSimilarity(datapointList.get(i), datapointList.get(j));
    			InteractionData interData = new InteractionData(StandardFormat.serialFormat(i, "00"), 
    					StandardFormat.serialFormat(j, "00"), 
    					v);
    			dataPointInteractions.add(interData);
    			vList.add(v);
    		}
    	}    	
    	//
    	double lambda = 0.5;
    	int iterationTimes = 5000;
    	int noChangeIterSpan = 20;    	
    	//double preferences = getMedian(vList);
    	double preferences = -4.0;
    	int preK = 2;
    	ArrayList<Double> dqRelevanceList = new ArrayList<Double>();
    	dqRelevanceList.add(1.0);
    	dqRelevanceList.add(2.0);
    	dqRelevanceList.add(1.0);
    	dqRelevanceList.add(2.0);
    	////
    	K_UFL kUFL = new K_UFL(lambda, iterationTimes, noChangeIterSpan, preferences, preK, UFLMode.C_Same_F, dataPointInteractions, dqRelevanceList);
    	//
    	kUFL.ini();
    	kUFL.run();    	
    }
	
	//
	public static void main(String []args){
		K_UFL.test();
	}
}
