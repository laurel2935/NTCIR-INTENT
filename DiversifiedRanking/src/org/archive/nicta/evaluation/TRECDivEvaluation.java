/** Code to load and evaluate TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

import org.archive.OutputDirectory;
import org.archive.a1.kernel.SBKernel;
import org.archive.a1.ranker.fa.DCKUFLRanker;
import org.archive.a1.ranker.fa.K_UFLRanker;
import org.archive.a1.ranker.fa.MDP;
import org.archive.a1.ranker.fa.MDP.fVersion;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.trec.TREC68Loader;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.doc.CLUEDoc;
import org.archive.dataset.trec.doc.Doc;
import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.evaluator.Evaluator;
import org.archive.nicta.evaluation.evaluator.TRECDivEvaluator;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEval10Losses;
import org.archive.nicta.kernel.BM25Kernel;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.kernel.LDAKernel;
import org.archive.nicta.kernel.PLSRKernel;
import org.archive.nicta.kernel.PLSRKernelTFIDF;
import org.archive.nicta.kernel.TF;
import org.archive.nicta.kernel.TFIDF;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nicta.ranker.mmr.MMR;
import org.archive.util.FileFinder;

////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on ClueWeb Content
////////////////////////////////////////////////////////////////////

public class TRECDivEvaluation {
	
	public static enum RankStrategy{BFS, MDP, FL}
	
	private static void trecDivEvaluation(DivVersion divVersion, RankStrategy rankStrategy){
		//output
		String output_prefix = OutputDirectory.ROOT+"results/DivEvaluation/";
		File outputFile = new File(output_prefix);
		if(!outputFile.exists()){
			outputFile.mkdirs();
		}
		
		String output_filename = null;
		if(DivVersion.Div2009 == divVersion){
			output_filename = "Div2009";			
		}else if(DivVersion.Div2010 == divVersion){
			output_filename = "Div2010";
		}else if(DivVersion.Div20092010 == divVersion) {
			output_filename = "Div20092010";
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}
		//cutoff
		int cutoffK = 20;
		//
		HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs();
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(divVersion);		
		Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(divVersion);		
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
		NDEval10Losses ndEval10Losses = new NDEval10Losses(TRECDivLoader.getTrecDivQREL(divVersion));		
		
		/************* Best First Strategy ***************/
		if(rankStrategy == RankStrategy.BFS){
			// Build a new result list selectors... all use the greedy MMR approach,
			// each simply selects a different similarity metric
			ArrayList<ResultRanker> rankers = new ArrayList<ResultRanker>();		
			// Instantiate all the kernels that we will use with the algorithms below
			//////////////////////
			//TF-Kernel
			//////////////////////
			///*
			//part-1
			Kernel TF_kernel    = new TF(trecDivDocs, 
					true //query-relevant diversity
					);
			Kernel TFn_kernel    = new TF(trecDivDocs, 
					false //query-relevant diversity
					);
			//part-2
			rankers.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TF_kernel // sim 
					, TF_kernel // div 
					));
			rankers.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TFn_kernel // sim
					, TFn_kernel // div
					));
			//*/
			////////////////////////////
			//BM25_kernel
			////////////////////////////
			/*
			//part-1 0 for any disables effect
			Kernel BM25_kernel  = new BM25Kernel(trecD0910Docs 				
					, 0.5d // k1 - doc TF
					, 0.5d // k3 - query TF
					, 0.5d // b - doc length penalty
					);
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
			Kernel PLSR_TFIDF_kernel = new PLSRKernelTFIDF(trecD0910Docs);
			//
			Kernel PLSR10_kernel  = new PLSRKernel(trecD0910Docs
					, 10 // NUM TOPICS - suggest 15
					, false // spherical
					);
			Kernel PLSR15_kernel  = new PLSRKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, 2.0
					, 0.5
					, false // spherical
					);
			Kernel PLSR15_sph_kernel  = new PLSRKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					);
			Kernel PLSR20_kernel  = new PLSRKernel(trecD0910Docs
					, 20 // NUM TOPICS - suggest 15
					, false // spherical
					);
			//part-2		
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR_TFIDF_kernel //sim
					, PLSR_TFIDF_kernel //div
					));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR15_kernel //sim
					, PLSR15_kernel //div
					));
			*/
			/*
			//part-1
			Kernel LDA10_kernel   = new LDAKernel(trecD0910Docs, 
					10 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);
			
			Kernel LDA15_kernel   = new LDAKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);

			Kernel LDA15_sph_kernel   = new LDAKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, false // query-relevant diversity
					);
			Kernel LDA15_qr_kernel   = new LDAKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, true // query-relevant diversity
					);
			Kernel LDA15_qr_sph_kernel   = new LDAKernel(trecD0910Docs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, true // query-relevant diversity
					);				
			//part-2	
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA10_kernel //sim
					, LDA10_kernel //div
					));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_kernel //sim
					, LDA15_kernel //div
					));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_sph_kernel //sim
					, LDA15_sph_kernel //div
					));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_kernel //sim
					, LDA15_qr_kernel //div
					));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_sph_kernel //sim
					, LDA15_qr_sph_kernel //div
					));		
			*/
			
			// Add all MMR test variants (vary lambda and kernels)
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename);
			try {
				trecDivEvaluator.doEval(TRECDivLoader.getDivEvalQueries(divVersion),
						trecDivDocs, trecDivQueryAspects, lossFunctions, rankers, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}else if(rankStrategy == RankStrategy.MDP){
			/************* note the number of topics for LDA training ****************/
			SBKernel _sbKernel = new SBKernel(trecDivDocs, 10);
			//
			MDP mdp = new MDP(trecDivDocs, 0.5d, _sbKernel, null, trecDivQueries);
			Vector<fVersion> mdpRuns = new Vector<MDP.fVersion>();
			mdpRuns.add(fVersion._dfa);
			mdpRuns.add(fVersion._dfa_scaled);
			mdpRuns.add(fVersion._md);
			mdpRuns.add(fVersion._md_scaled);
			mdpRuns.add(fVersion._pdfa);
			mdpRuns.add(fVersion._pdfa_scaled);
			mdpRuns.add(fVersion._pdfa_scaled_exp);
			mdpRuns.add(fVersion._pdfa_scaled_exp_head);					
			//
			try {
				mdp.doEval(TRECDivLoader.getDivEvalQueries(divVersion), trecDivDocs, trecDivQueryAspects,
					lossFunctions, cutoffK, output_prefix, output_filename, mdpRuns.toArray(new fVersion[0]));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}else if(rankStrategy == RankStrategy.FL){
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d; // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			
			//
			ArrayList<ResultRanker> rankers = new ArrayList<ResultRanker>();
			
			//1
			double lambda = 0.5;
			int iterationTimes = 5000;
			int noChangeIterSpan = 10; 
			DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda, iterationTimes, noChangeIterSpan);
			
			//2
			double SimDivLambda = 0.5;
			K_UFLRanker kuflRanker = new K_UFLRanker(trecDivDocs, tfidf_A1Kernel, lambda, iterationTimes, noChangeIterSpan, SimDivLambda);
			
			rankers.add(dckuflRanker);
			//rankers.add(kuflRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename);
			try {
				trecDivEvaluator.doEval(TRECDivLoader.getDivEvalQueries(divVersion),
						trecDivDocs, trecDivQueryAspects, lossFunctions, rankers, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		}		
	}
	//
	
	//
	public static void main(String []args){
		//DivVersion divVersion
		//RankStrategy rankStrategy
		TRECDivEvaluation.trecDivEvaluation(DivVersion.Div2009, RankStrategy.FL);
		
		
	}
}
