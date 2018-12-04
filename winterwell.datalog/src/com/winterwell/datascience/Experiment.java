package com.winterwell.datascience;

import java.io.File;
import java.util.List;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;

public class Experiment<Data, Model, Results> implements IHasDesc {
	
	transient Model model;
	Desc<Model> modelDesc;
	
	Results results;
	
	transient Data testData;	
	Desc<Data> testDataDesc;
	
	transient Data trainData;
	Desc<Data> trainDataDesc;

	@Override
	public Desc getDesc() {		
		Desc temp = new Desc(modelDesc.getName()+"-"+testDataDesc.getName(), Experiment.class);
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
	
}
