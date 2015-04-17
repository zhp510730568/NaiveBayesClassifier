package com.xh.text.app;

import java.io.IOException;

import com.datumbox.opensource.classifiers.NaiveBayes;
import com.datumbox.opensource.dataobjects.NaiveBayesKnowledgeBase;
import com.xh.text.corpus.CorpusExtractUtil;

public class Main {
	public static void main(String[] args) throws IOException {
        //train classifier
        NaiveBayes naiveBayes = new NaiveBayes();
        //naiveBayes.train("CHI", "D:/sensiword/稿件数据/分类语料/Bayes/corpus.txt", 0.02);
        naiveBayes.train("IG", "D:/NLP/中文文本分类新闻语料库/TanCorp-12-Txt/corpus.txt", 0.24);
        
        //get trained classifier knowledgeBase
        NaiveBayesKnowledgeBase knowledgeBase = naiveBayes.getKnowledgeBase();
        
        //Use classifier
        naiveBayes = new NaiveBayes(knowledgeBase);
        //String testResult = naiveBayes.predict("中国人民银行各分行、营业管理部、各省会（首府）城市中心支行、副省级城市中心支行；各省、自治区、直辖市住房城乡建设厅（建委）、银监局，新疆生产建设兵团建设局；各国有商业银行、股份制商业银行，中国邮政储蓄银行，直辖市、新疆生产建设兵团住房公积金管理委员会、住房公积金管理中心");
        //System.out.println("测试结果:" + testResult);
        // 测试正确率代码
        //CorpusExtractUtil.printCorrectRate(naiveBayes, "D:/sensiword/稿件数据/分类语料/Bayes/test.txt", "D:/sensiword/稿件数据/分类语料/Bayes/corpusResultTest.txt");
        CorpusExtractUtil.printCorrectRate(naiveBayes, "D:/NLP/中文文本分类新闻语料库/TanCorp-12-Txt/corpusTest.txt", "D:/NLP/中文文本分类新闻语料库/TanCorp-12-Txt/corpusTestResult.txt");
	}
}
