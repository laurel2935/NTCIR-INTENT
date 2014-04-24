package org.archive.nlp.chunk;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.archive.dataset.ntcir.sm.TaggedTerm;
import org.archive.dataset.ntcir.sm.TaggedTopic;
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
	private static final boolean DEBUG = true;

    private static final String[] en_options = { "-maxLength", "80", "-retainTmpSubcategories" };
    private static final String[] ch_options = { "-maxLength", "80"};
    
    private Lang lang = null;
    private String grammar = null;
    private LexicalizedParser lp = null;
    private TreebankLanguagePack tlp = null;
    //private GrammaticalStructureFactory gsf = null;
    
    
    public ShallowParser(Language.Lang lang){
    	if(Language.isEnglish(lang)){
    		this.grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    		this.lp = LexicalizedParser.loadModel(grammar, en_options);
    	}else if(Language.isChinese(lang)){
    		//this.grammar = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";   
    		this.grammar = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";   
    		this.lp = LexicalizedParser.loadModel(grammar, ch_options);
    	}else{
    		new Exception().printStackTrace();
    	}
    	//
    	this.lang = lang;    	
    	this.tlp = lp.getOp().langpack();
    	//this.gsf = tlp.grammaticalStructureFactory();
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
    
    public ArrayList<TaggedTerm> getTaggedTerms(String text){
    	ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    	
    	ArrayList<TaggedWord> taggedWords = getTaggedWords(text);
    	for(TaggedWord word: taggedWords){
    		taggedTerms.add(new TaggedTerm(word.value(), word.tag()));
    	}
    	
    	if(DEBUG){
    		System.out.println();
    		for(TaggedTerm taggedTerm: taggedTerms){
    			System.out.print(taggedTerm.toString()+"\t");
    		}
    		System.out.println();
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
    //
    public TaggedTopic getTaggedTopic(String topicText){
    	TaggedTopic taggedTopic = new TaggedTopic();
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(topicText));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree parse = lp.parse(sentence);
        //
        taggedTopic.setPennString(parse.pennString());
        //
        ArrayList<TaggedWord> taggedWords = parse.taggedYield();        
        //
        ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    	for(TaggedWord word: taggedWords){
    		taggedTerms.add(new TaggedTerm(word.value(), word.tag()));
    	}
    	taggedTopic.setTaggedTerms(taggedTerms);
    	//--    	
        boolean appear = false;
        ArrayList<Tree> treeSet = new ArrayList<Tree>();
    	//
    	List<Tree> leafSet = parse.getLeaves();
    	for(Tree leaf: leafSet){    		
    		//System.out.println("leaf:\t"+leaf.toString());
    		//tagged word tree
    		Tree parent = leaf.ancestor(1, parse);
    		//System.out.println("parent:\t"+parent.toString());
    		//
    		Tree ancestor = parent.ancestor(1, parse);
    		//System.out.println("ancestor:\t"+ancestor.toString());    		
    		//
    		if(null==ancestor || !isSimple2LevelTree(ancestor)){
    			treeSet.add(parent);
    		}else if(!treeSet.contains(ancestor)){
    			treeSet.add(ancestor);
    			if(!appear){
    				appear = true;
    			}    			
    		}   		
    	}
    	//   
    	if(appear){
    		ArrayList<TaggedTerm> taggedPhrases = new ArrayList<TaggedTerm>();
    		for(Tree tree: treeSet){
    			taggedPhrases.add(new TaggedTerm(getTerm(tree), tree.value()));
    		}    		       	
    		taggedTopic.setTaggedPhrases(taggedPhrases);
    	}else{
    		taggedTopic.setTaggedPhrases(null);
    	} 
    	//
    	return taggedTopic;        
    }
    //
    public ArrayList<TaggedTerm> getTaggedPhrases(String text){
    	ArrayList<Tree> treeSet = getSimple2LevelTrees(text);
    	if(null != treeSet){
    		ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    		for(Tree tree: treeSet){
    			taggedTerms.add(new TaggedTerm(getTerm(tree), tree.value()));
    		}
    		if(DEBUG){
        		System.out.println();
        		for(TaggedTerm taggedTerm: taggedTerms){
        			System.out.print(taggedTerm.toString()+"\t");
        		}
        		System.out.println();
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
    	if(childSet.size() < 2){
    		return false;
    	}
    	for(Tree child: childSet){
    		if(!isTaggedWordTree(child)){
    			return false;
    		}
    	}
    	return true;
    }    
    /**
     * 
     * **/
    private ArrayList<Tree> getSimple2LevelTrees(String text){
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
    			if(!appear){
    				appear = true;
    			}    			
    		}   		
    	}
    	//    	
    	/*
    	for(Tree tree: treeSet){
    		System.out.println(tree.yieldWords());
    		System.out.println(tree.value());
    		System.out.println(tree.taggedYield());
    		//System.out.println(tree.toString());
    	}
    	*/
    	if(appear){
    		//System.out.println(appear);
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
		ShallowParser shallowParser = new ShallowParser(Language.Lang.Chinese);
		shallowParser.test("相亲节目有哪些");
		//
		//System.out.println();
		//shallowParser.getSimple2LevelTrees("a sentence to be parsed", "NP");		
	}
}
