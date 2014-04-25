package org.archive.nlp.chunk.lpt.ltpService.examples;

import java.util.ArrayList;

import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.LTPOption;
import org.archive.nlp.chunk.lpt.ltpService.LTPService;
import org.archive.nlp.chunk.lpt.ltpService.Word;


public class Example3 {

    /**
     * <p>Title: main</p>
     * <p>Description: </p>
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        LTPService ls = new LTPService("email:token");

        try {
            LTML ltmlBeg = ls.analyze(LTPOption.WS,"����ҹ�������ǡ��Ƕ԰����һ��ڶг�������ǻۡ����߽��ĵ�ڤ�롣");
            //			ltml.printXml();
            LTML ltmlSec = new LTML();

            int sentNum = ltmlBeg.countSentence();
            for(int i = 0; i< sentNum; ++i){
                ArrayList<Word> wordList = ltmlBeg.getWords(i);
                for(int j = 0; j < wordList.size(); ++j){
                    System.out.print("\t" + wordList.get(j).getID());
                    System.out.print("\t" + wordList.get(j).getWS());
                    System.out.println();
                }

                //				merge
                ArrayList<Word> mergeList = new ArrayList<Word>();
                mergeList.add(wordList.get(0));
                Word mergeWord = new Word();
                mergeWord.setWS(wordList.get(1).getWS()+wordList.get(2).getWS());
                mergeList.add(mergeWord);
                //*
                for(int j = 3; j < wordList.size(); ++j){
                    Word others = new Word();
                    others.setWS(wordList.get(j).getWS());
                    mergeList.add(others);
                }

                ltmlSec.addSentence(mergeList, 0);
            }
            ltmlSec.setOver();
            System.out.println("\nmerge and get parser results.");
            LTML ltmlOut = ls.analyze(LTPOption.PARSER, ltmlSec);
            for(int i = 0; i< sentNum; ++i){
                ArrayList<Word> wordList = ltmlOut.getWords(i);
                for(int j = 0; j < wordList.size(); ++j){
                    System.out.print("\t" + wordList.get(j).getID());
                    System.out.print("\t" + wordList.get(j).getWS());
                    System.out.print("\t" + wordList.get(j).getPOS());
                    System.out.print("\t" + wordList.get(j).getParserParent() + 
                            "\t" + wordList.get(j).getParserRelation());

                    System.out.println();
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            ls.close();
        }
    }
}

