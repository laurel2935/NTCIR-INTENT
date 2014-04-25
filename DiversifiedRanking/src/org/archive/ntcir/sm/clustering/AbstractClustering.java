package org.archive.ntcir.sm.clustering;

import java.util.ArrayList;
import java.util.List;

import org.archive.dataset.ntcir.sm.SMSubtopicItem;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ntcir.sm.clustering.simfunction.SimilarityFunction;

public class AbstractClustering {
	
	public ArrayList<InteractionData> getInteractions(SMTopic smTopic){
		ArrayList<InteractionData> dataPoInteractions = new ArrayList<InteractionData>();
		ArrayList<SMSubtopicItem> smSubtopicItemList = smTopic.smSubtopicItemList;
		
		int n = smSubtopicItemList.size();
		for(int i=0; i<n-1; i++){
			SMSubtopicItem item_A = smSubtopicItemList.get(i);
			for(int j=i+1; j<n; j++){
				SMSubtopicItem item_B = smSubtopicItemList.get(j);
				double simValue = SimilarityFunction.calSimilarity(item_A, item_B);
				//
				dataPoInteractions.add(new InteractionData(Integer.toString(i), Integer.toString(j), simValue));
				dataPoInteractions.add(new InteractionData(Integer.toString(j), Integer.toString(i), simValue));
			}
		}
		
		return dataPoInteractions;
	}

}
