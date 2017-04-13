describe("HighchartsRenderer", function () {
	it("should create a global 'Chart.render' function", function () {
		expect(window.Chart).toBeDefined();
		
		expect(window.Chart.render).toBeDefined();
		
		expect(typeof window.Chart.render).toEqual("function");
	});
});

describe("Chart.render", function () {
	beforeEach(function () {
		spyOn(Highcharts, 'Chart');
	});
	
	it("should call delegate to Highcharts.chart", function () {
		Chart.render({});
	
		expect(Highcharts.Chart).toHaveBeenCalled();
	});
});
