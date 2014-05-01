package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.nlp.chunk.lpt.addon.LTPPosTag;
import org.archive.nlp.chunk.lpt.addon.LTPRelationTag;
import org.archive.nlp.chunk.lpt.addon.TNode;
import org.archive.nlp.chunk.lpt.addon.TTree;

public class LTPIRAnnotator {

	public ArrayList<IRAnnotation> irAnnotate(TTree tTree){
		ArrayList<IRAnnotation> results = new ArrayList<IRAnnotation>();
		//
		ArrayList<String> segmentations = parse(tTree);
		if(null != segmentations){
			for(String seg: segmentations){
				results.add(getIRAnnotation(seg, tTree));
			}
			return results;
		}else{
			return null;
		}		
	}
	
	//
	public IRAnnotation getIRAnnotation(String positionStr, TTree tTree){
		String [] array = positionStr.split(" ");
		Vector<TNode> nodeList = tTree.getNodes();
		if(array.length == 1){			
			int koID = Integer.parseInt(positionStr);
			TNode koNode = nodeList.get(koID); 
			KernelObject ko = new KernelObject(koNode.getContent(), koNode.getPosTag());
			//
			ArrayList<Modifier> moSet = new ArrayList<Modifier>();
			for(TNode moNode: nodeList){
				if(moNode.getID() != koID){
					moSet.add(new Modifier(moNode.getContent(), moNode.getPosTag()));
				}
			}
			//
			return new IRAnnotation(ko, moSet);
		}else{
			ArrayList<Integer> componentList = new ArrayList<Integer>();
			for(int i=0; i<array.length; i++){
				componentList.add(Integer.parseInt(array[i]));
			}
			//
			String koStr = "";
			for(Integer c: componentList){
				koStr += nodeList.get(c).getContent();
			}
			//
			KernelObject ko = new KernelObject(koStr, "unk");
			//
			ArrayList<Modifier> moSet = new ArrayList<Modifier>();
			for(TNode moNode: nodeList){
				if(!componentList.contains(moNode.getID())){
					moSet.add(new Modifier(moNode.getContent(), moNode.getPosTag()));
				}
			}
			//
			return new IRAnnotation(ko, moSet);			
		}
	}
	
	//
	public ArrayList<String> parse(TTree tTree){
		TNode root = tTree.getRoot();
		//one child
		if(!root.isLeaf() && 1==root.getChildren().size()){
			TNode child = root.getChildren().get(0);
			//n-ATT-
			if(root.getPosTag().equals(LTPPosTag.n) && child.getRelateTag().equals(LTPRelationTag.ATT)){				
				if(child.isLeaf()){	
					//n-ATT-Nh
					if(child.getPosTag().equals(LTPPosTag.nH)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child.getID()));
						return koList;
					}else if(child.getPosTag().equals(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						//i.e., n,n,np
						koList.add("0");koList.add("1");
						koList.add("0 1");
						return koList;
					}else{
						System.out.println("Unexcepted n-ATT-");
						return null;
					}					
				}else{
					//n-ATT-n(tree)
					if(child.getPosTag().equals(LTPPosTag.n)){
						return getNp(child);
					}else{
						System.out.println("Unexcepted n-ATT-(tree)");
						return null;
					}					
				}	
			//v-SBV
			}else if(root.getPosTag().equals(LTPPosTag.v) && child.getRelateTag().equals(LTPRelationTag.SBV)){				
				if(child.isLeaf()){
					//v-SBV-n
					if(child.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child.getID()));
						return koList;
					}else{
						System.out.println("Unexcepted v-SBV-");
						return null;
					}
				}else{
					//v-SBV-n(tree)
					if(child.getPosTag().startsWith(LTPPosTag.n)){
						return getNp(child);
					}else{
						System.out.println("Unexcepted v-SBV-(tree)");
						return null;
					}					
				}
			//r-ATT-
			}else if(root.getPosTag().equals(LTPPosTag.r) && child.getRelateTag().equals(LTPRelationTag.ATT)){
				if(child.isLeaf()){
					ArrayList<String> koList = new ArrayList<String>();
					koList.add(Integer.toString(child.getID()));
					return koList;
				}else {
					return getNp(child);
				}
			//v-VOB
			}else if(root.getPosTag().equals(LTPPosTag.v) && child.getRelateTag().equals(LTPRelationTag.VOB)){
				if(child.isLeaf()){
					ArrayList<String> koList = new ArrayList<String>();
					//n and vp
					koList.add("1");
					koList.add("0 1");
					return koList;
				}else{
					return getNp(child);
				}
			}else{
				System.out.println("Unexcepted one child case!");
				return null;
			}
		}else if(!root.isLeaf() && 2==root.getChildren().size()){			
			TNode child_1 = root.getChildren().get(0);
			TNode child_2 = root.getChildren().get(1);
			//SBV-v-VOB-r
			if(root.getPosTag().equals(LTPPosTag.v) 
					&& child_1.getRelateTag().equals(LTPRelationTag.SBV) && child_2.getRelateTag().equals(LTPRelationTag.VOB) 
					&& child_2.getPosTag().equals(LTPPosTag.r)){
				//
				if(child_1.isLeaf()){
					if(child_1.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child_1.getID()));
						return koList;
					}else{
						System.out.println("Unexpected ?-SBV-v-VOB-r");
						return null;
					}
				}else{
					System.out.println("Unexpected ?(tree)-SBV-v-VOB-r");
					return null;
				}
			//r-SBV-v-VOB
			}else if(root.getPosTag().equals(LTPPosTag.v) 
					&& child_1.getRelateTag().equals(LTPRelationTag.SBV) && child_2.getRelateTag().equals(LTPRelationTag.VOB) 
					&& child_1.getPosTag().equals(LTPPosTag.r)){
				//
				if(child_2.isLeaf()){
					if(child_2.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child_2.getID()));
						return koList;
					}else{
						System.out.println("Unexpected r-SBV-v-VOB-?");
						return null;
					}
				}else{
					System.out.println("Unexpected r-SBV-v-VOB-?(tree)");
					return null;
				}
			//v-n-n
			}else if(root.getPosTag().equals(LTPPosTag.v)){
				if(leaves(root.getChildren())){
					return getVp(root);
				}else {
					System.out.println("Unexcepted v - leaves!");
					return null;
				}
			//n-n-n
			}else if(root.getPosTag().equals(LTPPosTag.n)){
				if(leaves(root.getChildren())){
					return getNp(root);
				}else {
					System.out.println("Unexcepted n - leaves!");
					return null;
				}
			}else{
				System.out.println("Unexpected two child case!");
				return null;
			}
			//ATT & ATT
		}else{
			System.out.println("Unexcepted complex case!");
			return null;
		}		
	}
	//
	private static boolean leaves(Vector<TNode> children){
		for(TNode tNode: children){
			if(!tNode.isLeaf()){
				return false;
			}
		}
		return true;
	}
	//
	private ArrayList<String> getNp(TNode tree){
		//person root
		if(tree.getPosTag().equals(LTPPosTag.nH)){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID()));
			return koList;
		//single character root
		}else if(tree.getContent().length() == 1){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else if(tree.getPosTag().equals(LTPPosTag.n)){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID()));
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else{
			System.out.println("Unexpected np case!");
		}
		return null;		
	}
	//
	private ArrayList<String> getVp(TNode tree){
		//single character root
		if(tree.getContent().length() == 1){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else if(tree.getPosTag().equals(LTPPosTag.v)){
			ArrayList<String> koList = new ArrayList<String>();			
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else{
			System.out.println("Unexpected np case!");
		}
		return null;
	}
}
