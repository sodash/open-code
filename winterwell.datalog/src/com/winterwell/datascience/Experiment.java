package com.winterwell.datascience;

import java.io.File;
import java.util.List;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;
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
	Desc<Model> modelDesc;
	
	Results results;
	
	public Results getResults() {
		return results;
	}
	
	transient Data testData;	
	Desc<Data> testDataDesc;
	
	transient Data trainData;
	Desc<Data> trainDataDesc;
	
	private String tag = "experiment";

	@Override
	public Desc<Experiment<Data, Model, Results>> getDesc() {		
		Desc temp = new Desc(modelDesc.getName()+"-"+testDataDesc.getName(), Experiment.class);
		temp.setTag(tag);
		temp.addDependency("model", modelDesc);
		temp.addDependency("test", testDataDesc);
		if (trainDataDesc!=null) {
			temp.addDependency("train", trainDataDesc);
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
	
	public void setModel(Model model, Desc<Model> desc) {
		this.model = model;
		this.modelDesc = desc;
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
