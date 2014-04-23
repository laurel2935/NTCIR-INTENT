package org.archive.sm.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.archive.sm.data.IRAnnotation;
import org.archive.sm.data.KernelObject;
import org.archive.sm.data.Modifier;
import org.archive.sm.data.TaggedTerm;


public class SubAnnotator {
	//the entire subtopic string instances, and their corresponding tagged word list
	public HashMap<String, ArrayList<TaggedTerm>> _stInstance_all_noun = null;
	public HashMap<String, ArrayList<TaggedTerm>> _stInstance_all_np = null;
	//buffer of subtopic string's intent role annotation
	public HashMap<String, IRAnnotation> _irAnnotationCache = new HashMap<String, IRAnnotation>();	
	
	//
	public SubAnnotator(HashMap<String,ArrayList<TaggedTerm>> stInstance_all_noun, 
			HashMap<String,ArrayList<TaggedTerm>> stInstance_all_np) {
		this._stInstance_all_noun = stInstance_all_noun;	
		this._stInstance_all_np = stInstance_all_np;
	}
	//
	public IRAnnotation getIRAnnotation(String stInstance, KernelObject ko, boolean phraseLevel) {
		IRAnnotation irAnnotation = null;
		
		if ((irAnnotation = _irAnnotationCache.get(stInstance+ko.koStr)) != null){
			return irAnnotation;
		}
			
		ArrayList<TaggedTerm> taggedTerms;
		if(phraseLevel){
			taggedTerms = _stInstance_all_np.get(stInstance);
		}else{
			taggedTerms = _stInstance_all_noun.get(stInstance);
		}
		
		irAnnotation = getNoncachedIRAnnotation(taggedTerms, ko);
		_irAnnotationCache.put(stInstance, irAnnotation);
		return irAnnotation;
	}
	//
	public IRAnnotation getNoncachedIRAnnotation(ArrayList<TaggedTerm> taggedTerms, KernelObject ko){
		Vector<Modifier> moSet = new Vector<Modifier>();
		KernelObject matchedKO = null;
		boolean matched = false;
		for(TaggedTerm taggedTerm: taggedTerms){
			if(matched || !taggedTerm.koMatch(ko)){
				moSet.add(taggedTerm.toModifier());
			}else{
				matchedKO = taggedTerm.toKernelObject();
				matched = true;
			}
		}
		if(matched){
			return new IRAnnotation(matchedKO, moSet);
		}else{
			return new IRAnnotation(true);
		}
	}
}
