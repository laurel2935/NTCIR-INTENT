/** Code to load and evaluate TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import org.archive.OutputDirectory;
import org.archive.a1.ranker.fa.DCKUFLRanker;
import org.archive.a1.ranker.fa.DCKUFLRanker.Strategy;
import org.archive.a1.ranker.fa.MDP;
import org.archive.a1.ranker.fa.MDP.fVersion;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.nicta.evaluation.evaluator.Evaluator;
import org.archive.nicta.evaluation.evaluator.TRECDivEvaluator;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEval10Losses;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.kernel.LDAKernel;
import org.archive.nicta.kernel.PLSRKernel;
import org.archive.nicta.kernel.PLSRKernelTFIDF;
import org.archive.nicta.kernel.TF;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.nicta.ranker.BM25BaselineRanker;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nicta.ranker.mmr.MMR;

////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on ClueWeb Content
////////////////////////////////////////////////////////////////////

public class TRECDivEvaluation {
	
	public static enum RankStrategy{BFS, MDP, FL}
	
	//
	private static ArrayList<String> filterDivQuery(List<String> qList, Map<String,TRECDivQuery> divQueryMap, String typeStr){
		ArrayList<String> newQList = new ArrayList<String>();
		
		for(String q: qList){
			if(divQueryMap.get(q)._type.trim().equals(typeStr)){
				newQList.add(q);
			}
		}
		
		return newQList;
	}
	
	private static void trecDivEvaluation(DivVersion divVersion, RankStrategy rankStrategy){
		//differentiating faceted and ambiguous
		boolean diffFacetedAmbiguous = true;
		boolean acceptFaceted = false;
		String facetedType = "faceted";
		String ambType = "ambiguous";
		String typePrefix = "";	
		
		//cutoff
		int cutoffK = 20;
		//
		List<String> qList = TRECDivLoader.getDivEvalQueries(divVersion);
		HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs();		
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(divVersion);	
		
		if(diffFacetedAmbiguous){
			if(acceptFaceted){
				qList = filterDivQuery(qList, trecDivQueries, facetedType);
				typePrefix = "Faceted_";
			}else{
				qList = filterDivQuery(qList, trecDivQueries, ambType);
				typePrefix = "Amb_";
			}
		}
		
		Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(divVersion);
		
		//output
		String output_prefix = OutputDirectory.ROOT+"DivEvaluation/";
		File outputFile = new File(output_prefix);
		if(!outputFile.exists()){
			outputFile.mkdirs();
		}
		
		String output_filename = null;
		if(DivVersion.Div2009 == divVersion){
			
			output_filename = typePrefix+"Div2009"+rankStrategy.toString();		
			
		}else if(DivVersion.Div2010 == divVersion){
			
			output_filename = typePrefix+"Div2010"+rankStrategy.toString();
			
		}else if(DivVersion.Div20092010 == divVersion) {
			
			output_filename = typePrefix+"Div20092010"+rankStrategy.toString();
			
		}else{
			
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}
				
		// Build the Loss functions
		ArrayList<Metric> lossFunctions = new ArrayList<Metric>();
		// loss_functions.add(new USLoss());
		// loss_functions.add(new WSLoss());
		// loss_functions.add(new AvgUSLoss());
		// loss_functions.add(new AvgWSLoss());
		lossFunctions.add(new AllUSLoss());
		lossFunctions.add(new AllWSLoss());
		lossFunctions.add(new NDEval10Losses(TRECDivLoader.getTrecDivQREL(divVersion)));
		//
		//NDEval10Losses ndEval10Losses = new NDEval10Losses(TRECDivLoader.getTrecDivQREL(divVersion));		
				
		if(rankStrategy == RankStrategy.BFS){
			/*****************
			 * Best First Strategy
			 * ***************/
			//common
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();	
			// Build a new result list selectors... all use the greedy MMR approach,
			// each simply selects a different similarity metric				
			// Instantiate all the kernels that we will use with the algorithms below
			
			//////////////////////
			//TF-Kernel
			//////////////////////
			
			/*
			//part-1
			Kernel TF_kernel    = new TF(trecDivDocs, 
					true //query-relevant diversity
					);
			Kernel TFn_kernel    = new TF(trecDivDocs, 
					false //query-relevant diversity
					);
			//part-2
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TF_kernel // sim 
					, TF_kernel // div 
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TFn_kernel // sim
					, TFn_kernel // div
					));
			*/
			
			////////////////////////////
			//BM25 baseline
			////////////////////////////
			/*
			String nameFix = "_BM25Baseline";
			rankerList.add(new BM25BaselineRanker(trecDivDocs));	
			*/
			
			////////////////////////////
			//BM25_kernel
			////////////////////////////
			
			/*
			//for doc-doc similarity			
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			//for query-doc similarity
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);	
			
			String nameFix = null;
			boolean singleLambda = true;
			double weightedAvgLambda = Double.NaN;
			
			if(singleLambda){
				//single Lambda evaluation
				nameFix = "_BM25Kernel_A1+TFIDF_A1_SingleLambda";
				
				if(divVersion == DivVersion.Div2009){
					//using description
					//weightedAvgLambda =  0.542d;
					
					//no description derived from WT-2010
					weightedAvgLambda = 0.1813d;
				}else if(divVersion == DivVersion.Div2010){
					//using description
					//weightedAvgLambda =  0.3917d;
					
					//no description derived from WT-2009
					weightedAvgLambda = 0.272d;
				}else {
					System.err.println("Unsupported DivVersion!");
					System.exit(1);
				}
				
				rankerList.add(new MMR(trecDivDocs, 
						weightedAvgLambda //lambda: 0d is all weight on query sim
						, bm25_A1_Kernel // sim
						, tfidf_A1Kernel // div
						));				
			}else{
				//per Lambda evaluation
				//for similarity between documents, as bm25_A1_Kernel does not support				
				nameFix = "_BM25Kernel_A1+TFIDF_A1_PerLambda";
				for(int i=1; i<=11; i++){
					rankerList.add(new MMR(trecDivDocs, (i-1)/(10*1.0)
							, bm25_A1_Kernel // sim
							, tfidf_A1Kernel // div
							));
				}				
			}
			*/
			
			////////////////////////////
			//TFIDF_kernel
			////////////////////////////
			
			/*
			//part-1
			Kernel TFIDF_kernel = new TFIDF(trecD0910Docs,
					true //query-relevant diversity
					);
			Kernel TFIDFn_kernel = new TFIDF(trecD0910Docs, 
					false // query-relevant diversity
					);
			//part-2
			rankers.add( new MMR( trecD0910Docs, 
			0.5d //lambda: 0d is all weight on query sim
			, TFIDF_kernel // sim
			, TFIDF_kernel // div
			));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, TFIDFn_kernel //sim
					, TFIDFn_kernel //div
					));
			*/
			
			//////////////////////////
			//LDA-Kernel
			//////////////////////////	
			
			/*
			//part-1
			Kernel PLSR_TFIDF_kernel = new PLSRKernelTFIDF(trecDivDocs);
			//
			Kernel PLSR10_kernel  = new PLSRKernel(trecDivDocs
					, 10 // NUM TOPICS - suggest 15
					, false // spherical
					);
			Kernel PLSR15_kernel  = new PLSRKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, 2.0
					, 0.5
					, false // spherical
					);
			Kernel PLSR15_sph_kernel  = new PLSRKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					);
			Kernel PLSR20_kernel  = new PLSRKernel(trecDivDocs
					, 20 // NUM TOPICS - suggest 15
					, false // spherical
					);
			//part-2		
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR_TFIDF_kernel //sim
					, PLSR_TFIDF_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR15_kernel //sim
					, PLSR15_kernel //div
					));
			*/
			/*
			//part-1
			Kernel LDA10_kernel   = new LDAKernel(trecDivDocs, 
					10 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);
			
			Kernel LDA15_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);

			Kernel LDA15_sph_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, false // query-relevant diversity
					);
			Kernel LDA15_qr_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, true // query-relevant diversity
					);
			Kernel LDA15_qr_sph_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, true // query-relevant diversity
					);				
			//part-2	
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA10_kernel //sim
					, LDA10_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_kernel //sim
					, LDA15_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_sph_kernel //sim
					, LDA15_sph_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_kernel //sim
					, LDA15_qr_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_sph_kernel //sim
					, LDA15_qr_sph_kernel //div
					));		
			*/
			
			//////CIKM2014
			///*
			///*
			
//			Kernel LDA15_kernel   = new LDAKernel(trecDivDocs
//					, 15 // NUM TOPICS - suggest 15
//					, false // spherical
//					, true // query-relevant diversity for reference paper
//					);
			
			//Kernel plsrKernel = new PLSRKernel(trecDivDocs, 15, false);
			
			Kernel plsrKernel = new PLSRKernel(trecDivDocs, 15, 2.0, 0.5, false);			
			
			String nameFix = "_PLSR";			
			
			//single lambda evaluation
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, plsrKernel //sim
					, plsrKernel //div
					));	
			//*/
			//common: Add all MMR test variants (vary lambda and kernels)
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {				
				//
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
		}else if(rankStrategy == RankStrategy.MDP){	
			/////////////////////////
			//MDP run - style-1
			////////////////////////
			
			////single lambda evaluation
			/*
			////note the number of topics for LDA training
			//SBKernel _sbKernel = new SBKernel(trecDivDocs, 10);			
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			//
			int itrThreshold = 10000;
			
			String nameFix = "_SingleLambda";
			
			MDP mdp = new MDP(trecDivDocs, 0.5d, itrThreshold, tfidf_A1Kernel, null, trecDivQueries);
			
			Vector<fVersion> mdpRuns = new Vector<MDP.fVersion>();			
			mdpRuns.add(fVersion._dfa);
			//mdpRuns.add(fVersion._dfa_scaled);
			//mdpRuns.add(fVersion._md);
			//mdpRuns.add(fVersion._md_scaled);
			//mdpRuns.add(fVersion._pdfa);
			//mdpRuns.add(fVersion._pdfa_scaled);
			//mdpRuns.add(fVersion._pdfa_scaled_exp);
			//mdpRuns.add(fVersion._pdfa_scaled_exp_head);	
			try {				
				mdp.doEval(TRECDivLoader.getDivEvalQueries(divVersion), trecDivDocs, trecDivQueryAspects,
					lossFunctions, cutoffK, output_prefix, output_filename+nameFix, mdpRuns.toArray(new fVersion[0]));				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			*/			
						
			/////////////////////////
			//MDP run - style-2
			////////////////////////
			
			///*
			int itrThreshold = 10000;
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			
			boolean singleLambda = true;
			String nameFix = null;
			double weightedAvgLambda = Double.NaN;
			
			if(singleLambda){		
				//////single lambada evaluation
				nameFix = "_MDP_SingleLambda";
				//
				//double wt2009WeightedAvgLambda = 0.48d;
				//double wt2010WeightedAvgLambda = 0.5646d;
				if(divVersion == DivVersion.Div2009){
					//using description
					//weightedAvgLambda =  0.48d;
					
					//no description derived from wt-2010
					weightedAvgLambda = 0.55d;
					
				}else if(divVersion == DivVersion.Div2010){
					//using description
					//weightedAvgLambda =  0.5646d;
					
					//no description derived from WT-2009
					weightedAvgLambda = 0.43d;
				}else {
					System.err.println("Unsupported DivVersion!");
					System.exit(1);
				}
				
				MDP mdp = new MDP(trecDivDocs, weightedAvgLambda, itrThreshold, tfidf_A1Kernel, null, trecDivQueries);
				rankerList.add(mdp);
				
			}else{
				//////per Lambda evaluation
				//for similarity between documents, as bm25_A1_Kernel does not support
				nameFix = "_MDP_PerLambda";
				
				for(int i=1; i<=11; i++){
					//(i-1)/(10*1.0)
					rankerList.add(new MDP(trecDivDocs, (i-1)/(10*1.0), itrThreshold, tfidf_A1Kernel, null, trecDivQueries));
				}
			}
			
			// Evaluate results of different query processing algorithms			
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			//*/
			
		}else if(rankStrategy == RankStrategy.FL){
			
			//combination			
			ExemplarType exemplarType = ExemplarType.Y;
			Strategy flStrategy = Strategy.Belief;
			
			String nameFix = "_"+exemplarType.toString();
			nameFix += ("_"+flStrategy.toString());
						
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			//TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			
			//
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			
			//1
			double lambda_1 = 0.5;
			int iterationTimes_1 = 5000;
			int noChangeIterSpan_1 = 10; 
			//DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1);
			DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1, exemplarType, flStrategy);
			
			//2
			/*
			double lambda_2 = 0.5;
			int iterationTimes_2 = 10000;
			int noChangeIterSpan_2 = 10; 
			double SimDivLambda = 0.5;
			K_UFLRanker kuflRanker = new K_UFLRanker(trecDivDocs, tfidf_A1Kernel, lambda_2, iterationTimes_2, noChangeIterSpan_2, SimDivLambda);
			*/
			
			rankerList.add(dckuflRanker);
			//rankers.add(kuflRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		}		
	}
	//
	private static PrintStream printer = null; 
	public static void openPrinter(){
		try{
			printer = new PrintStream(new FileOutputStream(new File(OutputDirectory.ROOT+"DivEvaluation/"+"log.txt")));
			System.setOut(printer);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void closePrinter(){
		printer.flush();
		printer.close();
	}
	
	//
	public static void main(String []args){
		
		//DivVersion divVersion
		//RankStrategy rankStrategy
		
		//TRECDivEvaluation.openPrinter();		
		
		TRECDivEvaluation.trecDivEvaluation(DivVersion.Div2010, RankStrategy.FL);
		
		//TRECDivEvaluation.closePrinter();
		
		
	}
}
