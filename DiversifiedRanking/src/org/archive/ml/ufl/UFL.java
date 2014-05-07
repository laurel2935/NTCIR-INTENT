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

import com.sun.org.apache.bcel.internal.classfile.InnerClass;

public class UFL {
	
	private static final boolean debug = true;
	
	public enum UFLMode {C_Same_F, C_Differ_F}
	
	//basic parameters//
	//private static final double INF = 1000000000.0;
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;
    //given preference
    private double preferenceCost;
    //as the cost matrix takes the negative value of similarity matrix, thus ...
    //private boolean _logDomain;
    private ArrayList<InteractionData> _costMatrix;
    //f_j, i.e., negative value of d-q relevance
    private ArrayList<Double> _fList;
    
    //set of node identifier, i.e., names
    private Collection<String> _cNodeNames;
    private Collection<String> _fNodeNames;
    protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
    
    private UFLMode _uflMode;
    
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
	
	//exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D IX = null;    
    private IntegerMatrix1D IY = null;
	
    //the number of exemplar
    private int clustersNumber = 0;
	
    //pay attention to the positive or negative value of dataPointInteractions &&¡¡fCostList
    UFL(double lambda, int iterationTimes, Integer noChangeIterSpan, double preferenceCost, UFLMode uflMode, 
    		ArrayList<InteractionData> costMatrix, ArrayList<Double> fList){
    	//1
    	//dataPointSimilarities, for cost, e.g., c_ij, it would be the negative value of each similarity
    	//relevanceList, for facility f_j, it would be the negative value of each one
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;
    	this.preferenceCost = preferenceCost;
    	this._costMatrix = costMatrix;
    	this._fList = fList;    	
    	this._uflMode = uflMode;
    	//this._logDomain = logDomain;    	
    	//2
    	this.ini();
    }
    
    private void ini(){    	
    	this._cNodeNames = new HashSet<String>();
    	this._fNodeNames = new HashSet<String>();
    	for(InteractionData intData : this._costMatrix){
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
        
        for (InteractionData intData : this._costMatrix) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());          
            double c_ij = intData.getSim();            
            setCost(intData.getFrom(), intData.getTo(), c_ij);
        }        
        if(UFLMode.C_Same_F == _uflMode){
        	System.out.println("pref: " + preferenceCost);        
            for (int i = 0; i < this._N; i++) {
            	double c_ii = preferenceCost;
            	this._C.set(i, i, c_ii);
            }
        }
        //facility cost f_j
        this._Y = new DoubleMatrix2D(1, this._M, 0);
        for(int j=0; j<this._M; j++){
        	double f_j = this._fList.get(j);
        	this._Y.set(0, j, f_j);
        }
        
        //
        this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);        
        
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
    
    private Integer _cfID = 0;
    protected Map<String, Integer> _cfIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _cfIDRevMapper = new TreeMap<Integer, String>();
    
    protected Integer getCustomerID(String cName) {
    	if(UFLMode.C_Differ_F == this._uflMode){
    		if (_customerIDMapper.containsKey(cName)) {
                return _customerIDMapper.get(cName);
            } else {
                Integer id = _customerID;
                _customerIDMapper.put(cName, id);
                _customerIDRevMapper.put(id, cName);
                _customerID++;
                return id;
            }
    	}else{
    		if(_cfIDMapper.containsKey(cName)){
    			return _cfIDMapper.get(cName);
    		}else{
    			Integer id = _cfID;
    			_cfIDMapper.put(cName, id);
    			_cfIDRevMapper.put(id, cName);
    			_cfID++;
    			return id;
    		}
    	}        
    }
    protected String getCustomerName(Integer cID){
    	if(UFLMode.C_Differ_F == this._uflMode){    		
    		return this._customerIDRevMapper.get(cID);
    	}else{
    		return this._cfIDRevMapper.get(cID);
    	}
    }
    protected Integer getFacilityID(String fName){
    	if(UFLMode.C_Differ_F == _uflMode){
    		if(_facilityIDMapper.containsKey(fName)){
        		return _facilityIDMapper.get(fName);
        	}else{
        		Integer id = _facilityID;
        		_facilityIDMapper.put(fName, id);
        		_facilityIDRevMapper.put(id, fName);
        		_facilityID++;
        		return id;
        	}
    	}else{
    		if(_cfIDMapper.containsKey(fName)){
    			return _cfIDMapper.get(fName);
    		}else{
    			Integer id = _cfID;
    			_cfIDMapper.put(fName, id);
    			_cfIDRevMapper.put(id, fName);
    			_cfID++;
    			return id;
    		}
    	}    	
    }
    protected String getFacilityName(Integer fID){
    	if(UFLMode.C_Differ_F == this._uflMode){
    		return this._facilityIDRevMapper.get(fID);
    	}else{
    		return this._cfIDRevMapper.get(fID);
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
		if(debug){
			System.out.println("Computed beliefs:");
		}
		DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta).minus(this._C);
        //the indexes of potential exemplars
        IX = EX.diag().findG(0); 
        if(debug){
        	System.out.println("Selected Exemplars:");
        	for(Integer cID: IX.getVector()){
        		System.out.print(cID+"("+getCustomerName(cID)+")"+"\t");
        	}        	
        	System.out.println();
        }
        //
        DoubleMatrix1D EY;
        EY = this._V.minus(this._Y).getRow(0);
        IY = EY.findG(0);
        if(debug){
        	System.out.println("Selected Facilities:");
        	for(int fID: IY.getVector()){
        		System.out.print(fID+"("+getFacilityName(fID)+")"+"\t");
        	}
        	System.out.println();
        }
        
        if(debug){
        	System.out.println("Eta+Ro----------------!");
        	DoubleMatrix2D AR = this._Eta.minus(this._C).plus(this._Alpha);
        	DoubleMatrix2D maxAR = AR.maxr();
        	System.out.println("Maximum exemplars:");
        	for(int i=0; i<maxAR.getN(); i++){
        		//System.out.println(i+" -> "+(int)AR.get(i, 0));
        		if(i == (int)maxAR.get(i, 0)){
        			System.out.print(i+"("+getCustomerName(i)+")"+"\t");
        		}
        	}
        	System.out.println();
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
	//
	private void computeEta(){
		DoubleMatrix2D Alpha_minus_C = this._Alpha.minus(this._C);
		DoubleMatrix2D max = Alpha_minus_C.maxr();
		for(int i=0; i<this._N; i++){
			Alpha_minus_C.set(i, (int)max.get(i, 0), Double.NEGATIVE_INFINITY);
		}
		DoubleMatrix2D max2 = Alpha_minus_C.maxr();
		//
		double [] sameRow = new double [this._N];
		for(int i=0; i<this._N; i++){
			sameRow[i] = max.get(i, 1);
		}
		//
		DoubleMatrix2D maxElements = new DoubleMatrix2D(this._M, sameRow);
		DoubleMatrix2D zeroM = new DoubleMatrix2D(this._N, this._M, 0.0);
		//before real eta
		this._Eta = zeroM.minus(maxElements);
		//real eta
		for(int i=0; i<this._N; i++){
			this._Eta.set(i, (int)max.get(i, 0), 0.0-max2.get(i, 1));
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
	private void computeV_old(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		this._V = maxZero.sumEachColumn();		
	}
	private void computeV(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D cSum = maxZero.sumEachColumn();
		this._V = new DoubleMatrix2D(1, this._M, 0.0);
		for(int j=0; j<this._M; j++){
			this._V.set(0, j, 
					cSum.get(0, j)+this._Eta.get(j, j)-this._C.get(j, j));
		}		
	}
	//
	/*
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	*/
	
	//// Alpha ////
	private void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
		if(debug){
        	System.out.println("old Alpha:");
        	System.out.println(_oldAlpha.toString());
        }
	}
	private void computeAlpha_false(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		DoubleMatrix2D columnSum = maxZero.sumEachColumn();		
		//the value at ij should not be added
		DoubleMatrix2D sameColumnVector = columnSum.minus(this._Y);
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
	private void computeAlpha(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);		
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D maxZeroNoJJ = maxZero.sumEachColumn();
		//for i=j
		DoubleMatrix2D IisJ = maxZeroNoJJ.minus(this._Y);
		//--for i<>j		
		double [] row = new double [this._M];
		DoubleMatrix1D etaJJ = this._Eta.diag();
		DoubleMatrix1D cJJ = this._C.diag();
		for(int j=0; j<this._M; j++){
			row[j] = maxZeroNoJJ.get(0, j)+etaJJ.get(j)-cJJ.get(j)-this._Y.get(0, j);
		}		
		DoubleMatrix2D reversedM = new DoubleMatrix2D(this._N, row);
		DoubleMatrix2D rightM = reversedM.transpose();		
		DoubleMatrix2D rep = rightM.minus(maxZero);
		this._Alpha = rep.min(0);
		//
		for(int i=0; i<this._N; i++){
			this._Alpha.set(i, i, IisJ.get(0, i));
		}				
	}
	//
	private void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	
	
	protected void computeExemplars() {
        DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta).minus(this._C);
        //the indexes of potential exemplars
        this.IX = EX.diag().findG(0);
        if(debug){
        	System.out.println("Bigger Zero centers:");
        	System.out.println(IX.toString());
        }
        this.clustersNumber = this.IX.size();
        IntegerMatrix1D equalIX = EX.diag().findG_WithEqual(0);
        if(debug){
        	System.out.println("BiggerAndEqual Zero centers:");
        	System.out.println(equalIX.toString());
        }
        //
        DoubleMatrix2D maxAR = EX.maxr();
        if(debug){        	
        	System.out.println("Maximum centers:");
        	for(int i=0; i<maxAR.getN(); i++){
        		//System.out.println(i+" -> "+(int)AR.get(i, 0));
        		if(i == (int)maxAR.get(i, 0)){
        			System.out.print(i+" ");
        		}
        	}
        	System.out.println();
        }
        
        DoubleMatrix1D EY;
        EY = this._V.minus(this._Y).getRow(0);
        IY = EY.findG(0);
        if(debug){
        	System.out.println("BiggerZero Facilities:");
        	System.out.println(IY.toString());
        }
        IntegerMatrix1D equalIY = EY.findG_WithEqual(0);
        if(debug){
        	System.out.println("EqualZero Facilities:");
        	System.out.println(equalIY.toString());
        }
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
			//
			computeExemplars();
			
			calculateCovergence();
			
			if(!checkConvergence()){
				break;
			}
		}
		//
		this.copyV();
		this.computeV();
		//
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
	public static ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	//
	public static void testAPExample(){
		//similarity values
		ArrayList<InteractionData> costMatrix = getCostMatrix(APClustering.loadAPExample());
		//ArrayList<InteractionData> costMatrix = APClustering.loadAPExample();
    	//
    	double lambda = 0.5;
    	int iterationTimes = 100;
    	int noChangeIterSpan = 10;    	
    	//double preferences = getMedian(vList);
    	//positive value as a cost value
    	double costPreferences = 15.561256;    	
    	ArrayList<Double> fList = new ArrayList<Double>();
    	for(int j=0; j<25; j++){
    		fList.add(0.0);
    	}
    	////
    	UFL kUFL = new UFL(lambda, iterationTimes, noChangeIterSpan, costPreferences, UFLMode.C_Same_F, costMatrix, fList);
    	//    	
    	kUFL.run();    	
    }
	
	//
	public static void main(String []args){
		UFL.testAPExample();
	}
}
