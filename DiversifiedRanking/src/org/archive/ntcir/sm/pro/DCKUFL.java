package org.archive.ntcir.sm.pro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.archive.ml.clustering.ap.abs.ConvitsVector;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;
import org.archive.ml.ufl.K_UFL;
import org.archive.ml.ufl.K_UFL.UFLMode;
import org.archive.ml.ufl.Mat;
import org.archive.ntcir.sm.clustering.ap.APClustering;
import org.archive.util.tuple.DoubleInt;
import org.archive.util.tuple.PairComparatorByFirst_Desc;

public class DCKUFL {
	
	private static final boolean debug = true;
		
	//// Basic Parameters with default values ////	
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;
    
    
    //set of node identifier, i.e., names
    private Collection<String> _cNodeNames;
    private Collection<String> _fNodeNames;
    //
    private Integer _customerID = 0;
    protected Map<String, Integer> _customerIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _customerIDRevMapper = new TreeMap<Integer, String>();
    //
    private Integer _facilityID = 0;
    protected Map<String, Integer> _facilityIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _facilityIDRevMapper = new TreeMap<Integer, String>();
    
    protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
    
    
    
	///////////////////////
	// the corresponding index in the paper i,j  will be essentially i-1 j-1 in the program!
	///////////////////////
    //predefined k
    Integer G_M1_zM;
    
    //relevance matrix among subtopic and documents, positive values
    //private boolean _logDomain;
    private ArrayList<InteractionData> _releMatrix;
    
	
	//number of subtopics
	private int _N;
	//number of documents
	private int _M;
	//relevance matrix r_ij: the relevance of a subtopic i to document j
	private DoubleMatrix2D _R;
	//similarity matrix s_ij: the similarity of subtopic i to subtopic j
	private DoubleMatrix2D _S;
	//1¡ÁN one-row popularity vector: the popularity of each subtopic
	private DoubleMatrix1D _P;
	//1¡ÁN one-row capacity vector corresponding to each subtopic
	private DoubleMatrix1D _capList;
	
		
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
	
	//N¡Á(M+1)
	private DoubleMatrix2D _H;
	private DoubleMatrix2D _oldH;
	
	//exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D IX = null;    
    private IntegerMatrix1D IY = null;
	
    //the number of exemplar
    private int clustersNumber = 0;
    
    DCKUFL(double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK, int n, int m,
    		DoubleMatrix2D releMatrix, DoubleMatrix2D simMatrix, DoubleMatrix1D p, DoubleMatrix1D capList){
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;   
    	this.G_M1_zM = preK;    	
    	
    	this._N = n;
    	this._M = m;
    	this._R = releMatrix;
    	this._capList = capList;
    	this._P = p;
    	this._S = simMatrix;
    	
    	this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        this._H = new DoubleMatrix2D(this._N,this._M+1, 0);
        
        if(debug){
        	System.out.println("Rele matrix:");
        	System.out.println(_R.toString());        	
        }
    }
	
    //pay attention to the positive or negative value of dataPointInteractions &&¡¡fCostList
    DCKUFL(double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK, ArrayList<InteractionData> releMatrix){
    	//1
    	//dataPointSimilarities, for cost, e.g., c_ij, it would be the negative value of each similarity
    	//relevanceList, for facility f_j, it would be the negative value of each one
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;    	
    	this._releMatrix = releMatrix;    	
    	this.G_M1_zM = preK;    	
    	//this._logDomain = logDomain;    	
    	//2
    	this.ini();
    }
    
    private void ini(){    	
    	this._cNodeNames = new HashSet<String>();
    	this._fNodeNames = new HashSet<String>();
    	for(InteractionData intData : this._releMatrix){
        	this._cNodeNames.add(intData.getFrom());
        	this._fNodeNames.add(intData.getTo());
        }
    	this._N = this._cNodeNames.size();
		this._M = this._fNodeNames.size();     
        
        //rele matrix x_ij
        this._R = new DoubleMatrix2D(this._N, this._M, 0);
        
        for (InteractionData intData : this._releMatrix) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());          
            double r_ij = intData.getSim();            
            setRelevance(intData.getFrom(), intData.getTo(), r_ij);
        }    
        //
        this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        
        if(debug){
        	System.out.println("Rele matrix:");
        	System.out.println(_R.toString());        	
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
			this.updateAB();
			
			//
			this.copyGama();
			this.computeGama();
			this.updateGama();
			
			//
			this.copyH();
			this.computeH();
			
			//
			computeExemplars();
			
			calculateCovergence();
			
			if(!checkConvergence()){
				break;
			}
		}
		//
		computeBeliefs();
	}
    
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
    protected String getCustomerName(Integer cID){
    	return this._customerIDRevMapper.get(cID);
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
    protected String getFacilityName(Integer fID){
    	return this._facilityIDRevMapper.get(fID);
    }
    //
    public void setRelevance(final String from, final String to, final Double cost) {

        Integer cID = getCustomerID(from);
        Integer fID = getFacilityID(to);
        _R.set(cID, fID, cost.doubleValue());
    }
	
    public void computeBeliefs(){
		DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta);
        //the indexes of potential exemplars
        IX = EX.diag().findG(0); 
        if(debug){
        	System.out.println("Final ... >0 Selected Exemplars:");
        	for(Integer cID: IX.getVector()){
        		System.out.print(cID+"("+getCustomerName(cID)+")"+"\t");
        	}        	
        	System.out.println();
        }
        //
        DoubleMatrix2D EY;
        EY = this._V.plus(this._Gama);
        IY = EY.getRow(0).findG(0);
        if(debug){
        	System.out.println("Final ... >0  Facilities:");
        	for(int fID: IY.getVector()){
        		System.out.print(fID+"("+getFacilityName(fID)+")"+"\t");
        	}
        	System.out.println();
        }
	}
	
	public IntegerMatrix1D getSelectedDocs(){
		return this.IY;
	}	
	//return a negative value
	private double Fi(int i, int fNumber){
		if(0 == fNumber){
			return 0;
		}else if(fNumber > 0){
			double cap = this._capList.get(i);
			if(fNumber <= cap){
				return 0;
			}else{
				return -Math.exp(fNumber-cap);
			}			
		}else{
			new Exception("cNumber error!").printStackTrace();
			return Double.NEGATIVE_INFINITY;
		}		
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
		for(int i=0; i<this._N; i++){
			Vector<Double> IRow = this._Alpha.getRow(i).getVector();
			for(int j=0; j<this._M; j++){
				Vector<Double> irow = new Vector<Double>();
				irow.addAll(IRow);
				irow.remove(j);
				//
				ArrayList<DoubleInt> diList = Mat.getDoubleIntList(irow);
				Collections.sort(diList, new PairComparatorByFirst_Desc<Double, Integer>());
				ArrayList<Double> delS = Mat.cumsumDI(diList);
				//
				int m1=0;
				double maxV1 = Fi(i, 1);
				for(int m=1; m<=delS.size(); m++){
					double v1 = delS.get(m-1)+Fi(i, 1+m);
					if(v1 > maxV1){
						maxV1 = v1;
					}
				}
				//
				int m0=0;
				double maxV2 = Fi(i, 0);
				for(int m=1; m<=delS.size(); m++){
					double v2=delS.get(m-1)+Fi(i, m);
					if(v2 > maxV2){
						maxV2 = v2;
					}
				}
				//
				this._Eta.set(i, j, Math.min(maxV1, maxV1-maxV2));
			}
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
		for(int j=0; j<this._M; j++){
			double maxVk = Double.NEGATIVE_INFINITY;
			for(int k=0; k<this._N; k++){
				double vk = commonOperate(k, j)+this._Eta.get(k, j);
				if(vk > maxVk){
					maxVk = vk;
				}
			}
			this._V.set(0, j, maxVk);
		}	
	}
	// not used as V is only used for computing beliefs
	/*
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	*/
	
	//
	private double commonOperate(int rowI, int columnJ){
		ArrayList<Double> oneUniList = Mat.getUniformList(1.0, this._S.getN()-1);
		ArrayList<Double> sICoL = this._S.getColumn(rowI).getList();
		sICoL.remove(rowI);
		ArrayList<Double> minus_1 = Mat.minus(oneUniList, sICoL);
		//
		double PiRij = this._P.get(rowI)*this._R.get(rowI, columnJ);
		ArrayList<Double> pi_rij_UniList = Mat.getUniformList(PiRij, this._R.getN()-1);
		ArrayList<Double> rJCol = this._R.getColumn(columnJ).getList();
		rJCol.remove(rowI);
		ArrayList<Double> pList = this._P.getList();
		pList.remove(rowI);
		ArrayList<Double> pk_rkj_List = Mat.pointwiseMul(rJCol, pList);
		ArrayList<Double> minus_2 = Mat.minus(pi_rij_UniList, pk_rkj_List);
		//
		ArrayList<Double> mulList = Mat.pointwiseMul(minus_1, minus_2);
		double desiredSum = Mat.sum(mulList)+PiRij;
		return desiredSum;
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
		for(int j=0; j<this._M; j++){
			double gama_j = this._Gama.get(0, j);
			for(int i=0; i<this._N; i++){
				double iRow_jColumn_sum = commonOperate(i,j);
				//
				double maxV2 = Double.NEGATIVE_INFINITY;
				for(int k=0; k<this._N; k++){
					if(k != i){
						double v2 = commonOperate(k, j)+this._Eta.get(k, j);
						if(v2 > maxV2){
							maxV2 = v2;
						}
					}
				}				
				//
				this._Alpha.set(i, j, Math.min(iRow_jColumn_sum+gama_j,
						iRow_jColumn_sum-maxV2));
			}
		}	
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
		    				this._A.get(zj-1, j-1)+this._V.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }
	    //b-update
	    //bM(zM)=G_M1_zM
	    /*
	    for(int zM=0; zM<=this._M; zM++){
	    	this._B.set(zM, this._M, this.G_M1_zM);
	    }
	    */
	    this._B.set(this.G_M1_zM, this._M, this.G_M1_zM);
	    //
	    for(int j=this._M; j>=1; j--){
	    	for(int zj_minus_1=0; zj_minus_1<=this._M; zj_minus_1++){
	    		//
	    		if(this._M == zj_minus_1){
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
		    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)));
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
		
	//// H ////
	private void copyH(){
		this._oldH = this._H.copy();
		if(debug){
			System.out.println("old H:");
        	System.out.println(_oldH.toString()); 
		}
	}
	//
	private void computeH(){
		for(int i=0; i<this._N; i++){
			Vector<Double> aIRow = this._Alpha.getRow(i).getVector();
			ArrayList<DoubleInt> diList = Mat.getDoubleIntList(aIRow);
			Collections.sort(diList, new PairComparatorByFirst_Desc<Double, Integer>());
			ArrayList<Double> S = Mat.cumsumDI(diList);
			//
			this._H.set(i, 0, 0.0);			
			for(int ui=1; ui<=this._M; ui++){
				this._H.set(i, ui, S.get(ui-1));
			}
		}
	}
	
	protected void computeExemplars() {
		DoubleMatrix2D EX;
        EX = this._Alpha.plus(this._Eta);
        //the indexes of potential exemplars
        this.IX = EX.diag().findG(0);
        if(debug){
        	System.out.println("Iterating ... >0 exemplars[X]:");
        	System.out.println(IX.toString());
        }
        this.clustersNumber = this.IX.size();
        IntegerMatrix1D equalIX = EX.diag().findG_WithEqual(0);
        if(debug){
        	System.out.println("Iterating ... >=0 exemplars[X]:");
        	System.out.println(equalIX.toString());
        }
        //
        DoubleMatrix2D maxAR = EX.maxr();
        if(debug){        	
        	System.out.println("Iterating ... max exemplars[X]:");
        	for(int i=0; i<maxAR.getN(); i++){
        		//System.out.println(i+" -> "+(int)AR.get(i, 0));
        		if(i == (int)maxAR.get(i, 0)){
        			System.out.print(i+" ");
        		}
        	}
        	System.out.println();
        }
        //
        DoubleMatrix1D EY;
        EY = this._V.plus(this._Gama).getRow(0);
        IY = EY.findG(0);
        if(debug){
        	System.out.println("Iterating ... >0 Facilities[Y]:");
        	System.out.println(IY.toString());
        }
        IntegerMatrix1D equalIY = EY.findG_WithEqual(0);
        if(debug){
        	System.out.println("Iterating ... >= 0 Facilities[Y]:");
        	System.out.println(equalIY.toString());
        }
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
		Double [] capArray = {6.0, 4.0, 2.0, 1.0}; 
		DoubleMatrix1D capList = new DoubleMatrix1D(capArray);
		
		Double [] pArray = {0.4, 0.3, 0.2, 0.1};
		DoubleMatrix1D p = new DoubleMatrix1D(pArray);
		
		DoubleMatrix2D simMatrix = new DoubleMatrix2D(4, 4, 1.0);
		simMatrix.set(0, 1, 0.2);simMatrix.set(0, 2, 0.3);simMatrix.set(0, 3, 0.1);
		simMatrix.set(1, 0, 0.2);simMatrix.set(1, 2, 0.4);simMatrix.set(1, 3, 0.2);
		simMatrix.set(2, 0, 0.3);simMatrix.set(2, 1, 0.4);simMatrix.set(2, 3, 0.3);
		simMatrix.set(3, 0, 0.1);simMatrix.set(3, 1, 0.2);simMatrix.set(3, 2, 0.3);
		
		Random random = new Random();
		DoubleMatrix2D releMatrix = new DoubleMatrix2D(4, 50, 0.0);
		for(int i=0; i<4; i++){
			for(int j=0; j<50; j++){
				releMatrix.set(i, j, Math.abs(random.nextDouble()));
			}
		}
		//		
    	double lambda = 0.5;
    	int iterationTimes = 50;
    	int noChangeIterSpan = 10; 
    	int preK = 13;
    	ArrayList<Double> fList = new ArrayList<Double>();
    	for(int j=0; j<25; j++){
    		fList.add(0.0);
    	}
    	//double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK, int n, int m,
    	//DoubleMatrix2D releMatrix, DoubleMatrix2D simMatrix, DoubleMatrix1D p, DoubleMatrix1D capList
    	DCKUFL dckufl = new DCKUFL(lambda, iterationTimes, noChangeIterSpan, preK, 4, 50,
    			releMatrix, simMatrix, p, capList);
    	//    	
    	dckufl.run();    	
    }
	
	//
	public static void main(String []args){
		DCKUFL.testAPExample();
	}
}
