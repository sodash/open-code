package com.winterwell.datascience;

import java.util.List;
import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;
import com.winterwell.utils.log.Log;
/**
 * An Experiment is a specific repeatable test, with outputs.
 * It is specified by a trained model (i.e. model + training data) and the test-data.
 * 
 * Experiments can be stored e.g. in Depot or to file. 
 * They can carry transient data and models for convenience, 
 * but they do not store the data or model.
 * 
 * Recommended use: sub-class this to set the Data, Model, and Results types
 * 
 * @author daniel
 *
 * @param <Data>
 * @param <Model>
 * @param <Results>
 */
public class Experiment<Data, Model, Results> implements IHasDesc {		
	
	transient Model model;
	Desc<? extends Model> modelDesc;
	
	Results results;	
	
	transient Data testData;	
	Desc<Data> testDataDesc;
	
	transient Data trainData;
	Desc<Data> trainDataDesc;
	
	private String tag = "experiment";

	@Override
	public Desc getDesc() {
		Desc temp = new Desc("results", Experiment.class);
		temp.setTag(tag);
		temp.put("model", modelDesc.getName());
		temp.put("test", testDataDesc.getName());
		if (trainDataDesc!=null) {
			temp.put("train", trainDataDesc.getName());
		}
		return temp;
	}	
	
	public Model getModel() {
		return model;
	}
	
	public Data getTestData() {
		return testData; 
	}
	
	public Data getTrainData() {
		return trainData;
	}
	
	public void setModel(Model model, Desc<? extends Model> desc) {
		this.model = model;
		this.modelDesc = desc;
	}

	public Results getResults() {
		return this.results;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setResults(Results results) {
		this.results = results;
	}
			
	public void setTestData(Data testData, Desc<Data> testDataDesc) {
		this.testData = testData;
		this.testDataDesc = testDataDesc;
	}

	public void setTrainData(Data testData, Desc<Data> testDataDesc) {
		this.trainData = testData;
		this.trainDataDesc = testDataDesc;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+getDesc().getId()+"]";
	}
}

