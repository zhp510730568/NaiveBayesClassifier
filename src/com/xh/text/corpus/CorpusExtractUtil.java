package com.xh.text.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.datumbox.opensource.classifiers.NaiveBayes;

public class CorpusExtractUtil {

	/**
	 * 打印分类正确率，并输出原分类和贝叶斯分类的对比文件
	 * @param naiveBayes 贝叶斯分类对象
	 * @param testAriticlePath 测试集地址
	 * @param testAriticleOutpath 测试结果输出路径
	 * @return void
	 */
	public static void printCorrectRate(NaiveBayes naiveBayes, String testAriticlePath, String testAriticleOutpath) throws IOException {
		Map<String, Integer> testArtiCount = new HashMap<String, Integer>();
		Map<String, Integer> testArtiCorrectCount = new HashMap<String, Integer>();
		Map<String, Integer> testArtiCountOfCate = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(testAriticlePath)), "UTF-8"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testAriticleOutpath, false), "UTF-8"));
		String line = "";
		while((line = br.readLine()) != null) {
			String[] arr = line.split("\t", 2);
			if(arr.length == 2) {
				String classID = arr[0].trim();
				
				if(testArtiCount.containsKey(classID)) {
					testArtiCount.put(classID, testArtiCount.get(classID)  + 1);
				} else {
					testArtiCount.put(classID, 1);
				}
				String content = arr[1];
				String testResult = naiveBayes.predict(content);
				bw.write(String.format("%s\t%s\t%s\r\n", classID, testResult, content));
				if(testArtiCountOfCate.containsKey(testResult)) {
					testArtiCountOfCate.put(testResult, testArtiCountOfCate.get(testResult) + 1);
				} else {
					testArtiCountOfCate.put(testResult, 1);
				}
				if(classID.equals(testResult)) {
					if(testArtiCorrectCount.containsKey(classID)) {
						testArtiCorrectCount.put(classID, testArtiCorrectCount.get(classID) + 1);
					} else {
						testArtiCorrectCount.put(classID, 1);
					}
				}
			}
		}
		br.close();
		bw.close();
		
		// 打印各类别数量情况
		int totalCount = 0;
		int totalCorrCount = 0;
		for(int index = 0; index <= 12; index ++) {
			String classID = index + "";
			Integer cateCount = testArtiCount.get(classID);
			if(cateCount == null) {
				continue;
			}
			totalCount += cateCount;
			double precision = 0.0;
			double recall = 0.0;
			double F = 0.0;
			int corrCount = 0;
			if(testArtiCorrectCount.containsKey(classID)) {
				corrCount = testArtiCorrectCount.get(classID);
				totalCorrCount += corrCount;
				recall = corrCount * 1.0 / cateCount;
			}
			if(testArtiCountOfCate.containsKey(classID)) {
				int cateTestCount = testArtiCountOfCate.get(classID);
				precision = corrCount * 1.0 / cateTestCount;
			}
			
			if(recall > 0 && precision > 0) {
				F = recall * precision * 2 / (recall + precision);
			}
			System.out.println("ClassID:" + classID + "\t总数:" + cateCount + "\t召回率:" + recall + "\t准确率:" + precision + "\tF-测度值" + F);
		}
		System.out.println("总正确率:" + totalCorrCount * 1.0 / totalCount);
	}
	
	/**
	 * 合并语料，这主要是因为测试TanCorp这个手工语料时写的
	 * @param config 分词服务配置
	 * @param outputFilePath 输出文件路径
	 * @param testCountLimit 测试集文章的数量
	 * @return void
	 * @throws InterruptedException 
	 */
	public static void segAndMerCorpus(String inputFilePath, String outputDicPath, String outputTestFilePath, int testCountLimit) throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFilePath)), "UTF-8"));
		BufferedWriter bwTest = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputTestFilePath, true), "UTF-8"));
		Map<String, BufferedWriter> map = new HashMap<String, BufferedWriter>();
		
		Map<String, Integer> testArtiCountMap = new HashMap<String, Integer>();
		Map<String, String> nameMap = getClassIDAndNameMap();
		String line = "";
		while((line = br.readLine()) != null) {
			line = line.trim();
			if(line.equals("")) {
				continue;
			}
			String[] arr = line.split("\t", 2);
			if(arr.length == 2) {
				String classID = arr[0];
				String segContent = arr[1];
				if(testArtiCountMap.containsKey(classID)) {
					int currTestCount = testArtiCountMap.get(classID);
					if(currTestCount <= testCountLimit) {
						bwTest.write(String.format("%s\t%s\r\n", classID, segContent));
						testArtiCountMap.put(classID, currTestCount + 1);
						continue;
					} else {
						writeData(map, nameMap, classID, segContent, outputDicPath);
						testArtiCountMap.put(classID, currTestCount + 1);
					}
				} else {
					bwTest.write(String.format("%s\t%s\r\n", classID, segContent));
					testArtiCountMap.put(classID, 1);
					continue;
				}
			}
		}
		br.close();
		bwTest.close();
		closeWriter(map);
	}
	
	/**
	 * 创建缓冲区Writer
	 * @param path 文件路径
	 * @return BufferedWriter对象
	 */
	public static BufferedWriter createBufferedWriter(String path) throws UnsupportedEncodingException, FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false), "UTF-8"));
	}
	
	/**
	 * 根据类别名写数据到对应的输出流中
	 * @param map 输出流对应关系
	 * @param nameMap 输出文件所在地的目录
	 * @param classID 分词服务配置文件
	 * @param segcontent 语料某类文章的最大数量
	 * @param path 语料某类文章的最大数量
	 * @return void
	 */
	public static void writeData(Map<String, BufferedWriter> map, Map<String, String> nameMap, String classID, String segcontent, String path) throws IOException {
		BufferedWriter bw;
		if(map.containsKey(classID)) {
			bw = map.get(classID);
			bw.write(String.format("%s\r\n", segcontent));
			bw.flush();
		} else {
			String name = nameMap.get(classID);
			bw = createBufferedWriter(path + classID + "_" + name);
			bw.write(String.format("%s\r\n", segcontent));
			bw.flush();
			map.put(classID, bw);
		}
	}

	
	/**
	 * 关闭字典writer对象
	 * @param map 保存writer对象的字典
	 * @return void
	 */
	public static void closeWriter(Map<String, BufferedWriter> map) throws IOException {
		if(map == null) {
			return;
		}
		// 打印各类别数量情况
		Iterator<Entry<String, BufferedWriter>> iter = map.entrySet().iterator();
		BufferedWriter bw;
		while(iter.hasNext()) {
			Entry<String, BufferedWriter> entry = iter.next();
			bw = entry.getValue();
			if(bw != null) {
				bw.close();
			}
		}
	}

	
	/**
	 * 获得类标识和类别名称的对应关系，这主要用于生成
	 * @return 保存对应关系的字典
	 */
	public static Map<String, String> getClassIDAndNameMap() {
		Map<String, String> map = new HashMap<String, String>();
		// 国内政治
		map.put("0", "国内政治");
		// 国际政治
		map.put("1", "国际政治");
		// 社会
		map.put("2", "社会");
		// 财经
		map.put("3", "财经");
		// 娱乐
		map.put("4", "娱乐"); 
		// 体育
		map.put("5", "体育");
		// 科技
		map.put("6", "科技");
		// IT
		map.put("7", "IT");
		// 教育
		map.put("8", "教育");
		// 健康
		map.put("9", "健康");
		// 汽车
		map.put("10", "汽车");
		// 历史
		map.put("11", "历史");
		// 奇趣
		map.put("12", "奇趣");
		
		return map;
	}
}
