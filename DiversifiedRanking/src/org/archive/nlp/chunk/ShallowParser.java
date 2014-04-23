package org.archive.nlp.chunk;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.archive.sm.data.TaggedTerm;
import org.archive.util.Language;
import org.archive.util.Language.Lang;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class ShallowParser {

    private static String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };
    
    private Lang lang = null;
    private String grammar = null;
    private LexicalizedParser lp = null;
    private TreebankLanguagePack tlp = null;
    private GrammaticalStructureFactory gsf = null;
    
    
    public ShallowParser(Language.Lang lang){
    	if(Language.isEnglish(lang)){
    		this.grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    	}else if(Language.isChinese(lang)){
    		this.grammar = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";    		
    	}else{
    		new Exception().printStackTrace();
    	}
    	//
    	this.lang = lang;
    	this.lp = LexicalizedParser.loadModel(grammar, options);
    	this.tlp = lp.getOp().langpack();
    	this.gsf = tlp.grammaticalStructureFactory();
    }
    
    /**
     * the tokenized word and its pos tag
     * **/
    private ArrayList<TaggedWord> getTaggedWords(String text){
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree parse = lp.parse(sentence);
        //
        return parse.taggedYield();
    } 
    
    public ArrayList<TaggedTerm> getTaggedTerms_Noun(String text){
    	ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    	
    	ArrayList<TaggedWord> taggedWords = getTaggedWords(text);
    	for(TaggedWord word: taggedWords){
    		taggedTerms.add(new TaggedTerm(word.value(), word.tag()));
    	}
    	
    	return taggedTerms;
    }
    //
    private String getTerm(Tree tree){
    	String term = "";
    	ArrayList<Word> words = tree.yieldWords();
    	if(Language.isEnglish(lang)){
    		term += words.get(0).word();
    		for(int i=1; i<words.size(); i++){
    			term = term + " " + words.get(i).word();
    		}
    		return term;
    	}else{    		
    		for(int i=0; i<words.size(); i++){
    			term += words.get(i).word();
    		}
    		return term;
    	}
    }
    
    public ArrayList<TaggedTerm> getTaggedTerms_Np(String text){
    	ArrayList<Tree> treeSet = getSimple2LevelTrees(text, "NP");
    	if(null != treeSet){
    		ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    		for(Tree tree: treeSet){
    			taggedTerms.add(new TaggedTerm(getTerm(tree), tree.value()));
    		}
    		return taggedTerms;
    	}else{
    		return null;
    	}
    }
    /**
     * It finds that: the leaves are a bare word, namely without pos tag
     * its parent is a merely a tagged word, also a tree!
     * e.g., for the word "a", its parent is just "DT a"
     * **/
    
    /**
     * the tagged word tree, whose child is merely a bare word
     * **/
    private boolean isTaggedWordTree(Tree tree){
    	if(tree.isLeaf()){
    		return false;
    	}
    	List<Tree> childSet = tree.getChildrenAsList();
    	for(Tree child: childSet){
    		if(!child.isLeaf()){
    			return false;
    		}
    	}
    	return true;
    }
    //whose children are all tagged word tree
    /** an example: a sentence to be parsed
     *  leaf:	a
		parent:	(DT a)
		ancestor:	(NP (DT a) (NN sentence))
		leaf:	sentence
		parent:	(NN sentence)
		ancestor:	(NP (DT a) (NN sentence))
		leaf:	to
		parent:	(TO to)
		ancestor:	(VP (TO to) (VP (VB be) (VP (VBN parsed))))
		leaf:	be
		parent:	(VB be)
		ancestor:	(VP (VB be) (VP (VBN parsed)))
		leaf:	parsed
		parent:	(VBN parsed)
		ancestor:	(VP (VBN parsed))  // special case!!!!!!!!!
     * **/
    private boolean isSimple2LevelTree(Tree tree){
    	if(tree.isLeaf() || isTaggedWordTree(tree)){
    		return false;
    	}
    	List<Tree> childSet = tree.getChildrenAsList();
    	for(Tree child: childSet){
    		if(!isTaggedWordTree(child)){
    			return false;
    		}
    	}
    	return true;
    }
    //
    private boolean treeFilter(Tree tree, String type){
    	if(type.length() > 0){
    		if(tree.value().equals(type)){
    			return true;
    		}else{
    			return false;
    		}
    	}else{
    		return true;
    	}
    }
    /**
     * 
     * **/
    private ArrayList<Tree> getSimple2LevelTrees(String text, String type){
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree rootTree = lp.parse(sentence);
        //
        boolean appear = false;
        ArrayList<Tree> treeSet = new ArrayList<Tree>();
    	//
    	List<Tree> leafSet = rootTree.getLeaves();
    	for(Tree leaf: leafSet){    		
    		//System.out.println("leaf:\t"+leaf.toString());
    		//tagged word tree
    		Tree parent = leaf.ancestor(1, rootTree);
    		//System.out.println("parent:\t"+parent.toString());
    		//
    		Tree ancestor = parent.ancestor(1, rootTree);
    		//System.out.println("ancestor:\t"+ancestor.toString());    		
    		//
    		if(null==ancestor || !isSimple2LevelTree(ancestor)){
    			treeSet.add(parent);
    		}else if(!treeSet.contains(ancestor)){
    			treeSet.add(ancestor);
    			if(!appear && treeFilter(ancestor, type)){
    				appear = true;
    			}    			
    		}   		
    	}
    	//    	
    	///*
    	for(Tree tree: treeSet){
    		System.out.println(tree.yieldWords());
    		System.out.println(tree.value());
    		System.out.println(tree.taggedYield());
    		//System.out.println(tree.toString());
    	}
    	//*/
    	if(appear){
    		System.out.println(appear);
    		return treeSet;
    	}else{
    		return null;
    	}    	
    }
        
    
    public void test(String testString){    	
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(testString));
        List<? extends HasWord> sentence = toke.tokenize();
        List<List<? extends HasWord>> tmp = new ArrayList<List<? extends HasWord>>();  
        //
        System.out.println(sentence.toString());
        //        
        Tree parse = lp.parse(sentence);
        
        parse.pennPrint();
        System.out.println();
        
		//        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		//        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		//        
		//        System.out.println(tdl);
		//        System.out.println();
		//
		//        System.out.println("The words of the sentence:");
		//        for (Label lab : parse.yield()) {
		//          if (lab instanceof CoreLabel) {
		//            System.out.println(((CoreLabel) lab).toString("{map}"));
		//          } else {
		//            System.out.println(lab);
		//          }
		//        }
        
        System.out.println();
        System.out.println(parse.taggedYield());
        ArrayList<TaggedWord> taggedWords = parse.taggedYield();
        for(TaggedWord word: taggedWords){
        	System.out.println(word.tag() + "\t" + word.value());
        }
        System.out.println();
    }
	
	
	//
	public static void main(String []args){
		//1
		ShallowParser shallowParser = new ShallowParser(Language.Lang.English);
		shallowParser.test("a sentence to be parsed");
		//
		System.out.println();
		shallowParser.getSimple2LevelTrees("a sentence to be parsed", "NP");		
	}
}
