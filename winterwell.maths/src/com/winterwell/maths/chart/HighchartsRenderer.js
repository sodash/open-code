// See https://github.com/umdjs/umd/blob/master/amdWeb.js
(function (root, factory) {
	if (typeof define === 'function' && define.amd) {
		// AMD. Register as an anonymous module.
		define(['jquery', 'Highcharts'], factory);
	} else {
		// Browser globals
		root.Chart = factory(root.jQuery, root.Highcharts);
	}
}(this, function ($, Highcharts) {
/**
 * A wrapper for Highcharts that adheres to the Winterwell charting library API.
 * 
 * @author Steven King <steven@winterwell.com>
**/
	var previousPageX = null, // For dragging data-points; used to calaulate drag delta.
		previousPageY = null, // For dragging data-points; used to calaulate drag delta.
		defaultChartOptions, // Set for all charts.
		defaultLinkedChartOptions; // Set for all linked charts.
	
	// See http://api.highcharts.com/highcharts for details.
	defaultChartOptions = {
		plotOptions: {
			series: {
				events: {
					legendItemClick: onLegendItemClick
				},
				point: {
					events: {
						click: onPointClick
					}
				}
			},
			pie: {
				point: {
					events: {
						legendItemClick: onLegendItemClick
					}
				},
				dataLabels: {
					distance: 5,
					connectorWidth: 2,
					enabled: true,
					color: '#000000',
					formatter: formatPieChartLabels
				}
			}
		}
	};
	
	defaultLinkedChartOptions = {
		plotOptions: {
			series: {
				events: {
					legendItemClick: onLinkedSeriesLegendItemClick
				}
			},
			pie: {
				point: {
					events: {
						legendItemClick: onLinkedPieLegendItemClick
					}
				}
			}
		},
		legend: {
			maxHeight: 75
		}
	};
	
	Highcharts.setOptions({
		global: {
			useUTC: false // Displays datetime axes using the local timezone.
		}
	});
	
	return {
/**
 * Render the chart or linked-charts.
 * 
 * @param {Object|Object[]} chart The chart(s) to render, in the form specified
 * by the Winterwell charting API. If an array of charts is passed, they will
 * be rendered as linked-charts.
 * @param {Object|Object[]} options A map of library-specific options to override
 * those supplied by chart. An array of options should be passed if the charts
 * are also passed as an array.
 * @returns {Highcharts.Chart|Highcharts.Chart[]} The rendered chart(s).
**/
		render: function (chart, options) {
			if ($.isArray(chart)) {
				if (chart.length > 1) {
					return renderLinkedCharts(chart, options);
				} else {
					// Don't link a single chart.
					return renderChart(chart[0], options[0]);
				}
			} else {
				return renderChart(chart, options);
			}
		}
	};
	
/**
 * Handler for clicks on a chart point. Toggles the visibility of the tooltip
 * for the point.
**/
	function onPointClick(event) {
		toggleDisplayedPoint(this);
	}
	
/**
 * Handler for clicks on a legend. Ensures that any displayed points for a series
 * that is being hidden are removed.
**/
	function onLegendItemClick(event) {
		hideDisplayedPoints(this);
	}
	
/**
 * Handler for clicks on a legend of a (non-pie-) chart that is linked. Updates
 * the series' toggle-state across all linked charts, and handles the removing
 * of any relevant displayed points.
**/
	function onLinkedSeriesLegendItemClick(event) {
		event.preventDefault(); // We handle toggling the clicked series manually.
		
		toggleLinkedChartSeries(this, this.chart.options.linkedCharts);
	}
	
/**
 * Handler for clicks on a legend of a pie-chart that is linked. Updates the
 * series' toggle-state across all linked charts, and handles the removing of
 * any relevant displayed points.
**/
	function onLinkedPieLegendItemClick(event) {
		event.preventDefault(); // We handle toggling the clicked series manually.
		
		toggleLinkedChartSeries(this, this.series.chart.options.linkedCharts);
	}
	
/**
 * Handler for clicking on displayed point tooltips, to being dragging them.
**/
	function onDisplayedPointMousedown(event) {
		previousPageX = event.pageX;
		
		previousPageY = event.pageY;
		
		$(this).on('mousemove', onDisplayedPointMousemove); 
	}
	
/**
 * Handler for releasing displayed point tooltips that are being dragged.
**/
	function onDisplayedPointMouseup(event) {
		previousPageX = null;
		
		previousPageY = null;
		
		$(this).off('mousemove', onDisplayedPointMousemove);
	}
	
/**
 * Handler for dragging displayed point tooltips. We set the SVG `transform`
 * attribute using the `translate` definition to update the position. See
 * https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform for
 * details.
 * 
 * @param {Object} chart The map of options to parse.
 * @returns {Object} The updated map.
**/
	function onDisplayedPointMousemove(event) {
		var offset = $(this).offset(),
			position = $(this).position(),
			transform = {};
		
		transform.left = position.left + event.pageX - previousPageX - 0.5;
		
		transform.top = position.top + event.pageY - previousPageY - 0.5;
		
		$(this).attr('transform', 'translate(' + transform.left + ',' + transform.top + ')');
		
		previousPageX = event.pageX;
		
		previousPageY = event.pageY;
	}
	
/**
 * Rounds a number to a specified precision, but not if the result would
 * modify the non-fraction part. For example:
 * 
 *     restrictFractionalPart(31415, 3) === '31415' // true
 *     restrictFractionalPart(0.031415, 3) === '0.0314' // true
**/
	function restrictFractionalPart(val, n) {
		n = n || 1;
		
		if (val >= Math.pow(10, n)) {
			return Math.round(val);
		} else {
			return val.toPrecision(n);
		}
	}
	
/**
 * A default formatter for pie-chart labels. Displays value and percentage. Does
 * not display a label for slices < 5% of the total.
 * 
 * @see http://api.highcharts.com/highcharts#plotOptions.pie.dataLabels.formatter
**/
	function formatPieChartLabels() {
		if (Math.round(this.point.percentage) < 5) {
			return null;
		}
		
		return '<span style="font-size: 8pt; font-family: Arial, sans-serif;">' + 
			restrictFractionalPart(this.point.y) + ' (' + Math.round(this.point.percentage) + '%)</span>';
	}
	
/**
 * For the given series, hides any displayed points.
 * 
 * @param {Object} series - The series for which points should be hidden.
**/
	function hideDisplayedPoints(series) {
		var i,
			j,
			points;
		
		points = getDisplayedPointsBySeries(series);
		
		for (i = 0, j = points.length; i < j; i++) {
			hidePoint(points[i]);
		}
	}
	
/**
 * Returns any points that are currently displaying a tooltip for the supplied series.
 * 
 * @param {Object} The series for which points should be obtained.
 * @param {Object[]} An array of currently displayed points.
**/
	function getDisplayedPointsBySeries(series) {
		var point,
			result = [];
		
		for (i = 0, j = series.points.length; i < j; i++) {
			point = series.points[i];
			
			if (point.isTooltipDisplayed === true) {
				result.push(point);
			}
		}
		
		return result;
	}
	
/**
 * Toggles the dislpay state for a point's tooltip.
 * 
 * @param {Object} The point for which the tooltip's display state should be toggled.
**/
	function toggleDisplayedPoint(point) {
		if (point.isTooltipDisplayed === true) {
			hidePoint(point);
		} else {
			showPoint(point);
		}
	}
	
/**
 * Shows the tooltip for a point.
 * 
 * @param {Object} point - The point for which the tooltip should be displayed.
**/
	function showPoint(point) {
		var chart = point.series.chart;
		
		point.isTooltipDisplayed = true;
		
		point.tooltipElement = $(chart.tooltip.label.element)
			.clone()
			.hide()
			.on('mousedown', onDisplayedPointMousedown)
			.on('mouseup mouseleave', onDisplayedPointMouseup);
		
		$(chart.container).children().first().append(point.tooltipElement);
		
		point.tooltipElement.fadeIn();
	}
	
/**
 * Hides the tooltip for a point.
 * 
 * @param {Object} point - The point for which the tooltip should be hidden.
**/
	function hidePoint(point) {
		delete point.isTooltipDisplayed;
		
		point.tooltipElement.fadeOut(function () {
			$(this)
				.remove()
				.off();
			
			delete point.tooltipElement;
		});
	}
	
/**
 * Parses a map of options, so that it complies with the Highcharts API.
 * 
 * @param {Object} chart The map of options to parse.
 * @returns {Object} The updated map.
**/
	function parseChartData(chart) {
		var colors = Highcharts.getOptions().colors;
		
		// Move any series colors to the top-level 'colors' array.
		if ($.isArray(chart.series)) {
			$.map(chart.series, function (element, index) {
				if (element.color) {
					colors[index % 10] = element.color;
					
					delete element.color;
				}
				
				return element;
			});
			
			chart.colors = colors;
		}
		
		chart.credits = {
			enabled: false
		};
		
		return $.extend(true, {}, defaultChartOptions, chart);
	}
	
/**
 * Renders a single chart, using Highcharts.
 * 
 * @param {Object} chart The chart to render, using the options offered by the
 * Winterwell charting API.
 * @param {Object} [options] Any additional chart rendering options.
 * @returns {Highcharts.Chart} The rendered chart, with added promise behvaiour.
 * The resolution-state of the promise reflects whether the chart displays any
 * data.
**/
	function renderChart(chart, options) {
		var renderedChart,
			seriesData,
			params;
		
		if (!options) {
			options = {};
		}
		
		chart = parseChartData(chart);
		
		// We don't want to display the chart data on the intial rendering, as
		// we want to allow `addSeries` to process the series data after the
		// chart is rendered.
		if (chart.series) {
			seriesData = chart.series;
			
			delete chart.series;
		}
		
		if (chart.urlParams) {
			params = chart.urlParams || {};
			
			delete chart.urlParams;
		}
		
		renderedChart = new Highcharts.Chart($.extend(true, chart, options));
		
		// Restore the chart series data, as it could be used externally.
		chart.series = seriesData;
		
		if (typeof seriesData === 'string') {
			renderedChart.showLoading();
			
			renderedChart = $.ajax({
				chart: renderedChart,
				url: seriesData,
				data: params,
				dataType: 'json'
			}).then(function (data) {
				// Modify the promise state depending on whether data could be
				// added to the chart or not.
				return addSeries(renderedChart, data.cargo, chart.parseData);
			// Add the promise functionality to the rendered-chart object.
			}).promise(renderedChart);
		} else {
			// Add the promise functionality to the rendered-chart object. The
			// promise resolution is handled by `addSeries`.
			renderedChart = addSeries(renderedChart, seriesData, chart.parseData)
				.promise(renderedChart);
		}
		
		return renderedChart.fail(function () {
			$chart = $('#' + renderedChart.options.chart.renderTo);
			
			// Indicate that the chart contains no data.
			$chart.addClass('no-data');
		});
	}
	
/**
 * Renders multiple linked charts, using Highcharts.
 * 
 * @param {Object[]} linkedCharts - The charts to render, using the options
 * offered by the Winterwell charting API.
 * @param {Object[]} [options] - Any additional chart rendering options, for
 * each chart.
 * @returns {Highcharts.Chart[]} The rendered charts.
**/
	function renderLinkedCharts(linkedCharts, options) {
		var renderedCharts = [];
		
		if (!options) {
			options = [];
		}
		
		$.each(linkedCharts, function (index, chart) {
			if (!options[index]) {
				options[index] = {};
			}
			
			// Render the chart, and add it to the array of charts.
			renderedCharts.push(renderChart(chart, $.extend(true, {}, options[index], defaultLinkedChartOptions)));
		});
		
		$.each(renderedCharts, function (index, chart) {
			// Add information to the options of each chart regarding the other
			// linked charts.
			chart.options.linkedCharts = renderedCharts;
			
			// If the promise-state of a chart is rejected, it means that chart
			// does not contain any data. We update the classes on that chart,
			// and all linked charts, so that the chart with no data can be
			// hidden, and other charts can occupy the available space.
			chart.fail(function () {
				$.each(renderedCharts, function (otherIndex, otherChart) {
					$chart = $('#' + otherChart.options.chart.renderTo);
					
					if (otherIndex !== index) {
						// Indicates that this chart has a *sibling* that has
						// no data.
						$chart.addClass('sibling-no-data');
					}
					
					// Tell the chart that it has more space to expand into.
					otherChart.reflow();
				});
			});
		});
		
		return renderedCharts;
	}
	
/**
 * Adds a series, or array of series, to a chart. The chart should be rendered
 * to allow the dimensions of the content area to be accessed. If there is no
 * useful data to add to the chart, the message 'No data available' is displayed.
 * 
 * @param {Highcharts.Chart} chart - The chart to which the series should be
 * added.
 * @param {Object|Object[]} data - The series, or array of series, that should
 * be added to the chart.
 * @param {Function} [parseData] - Data-parsing function.
 * @returns A http://api.jquery.com/jQuery.Deferred/. If data is added to the
 * chart, the deferred is resolved, if not then it is rejected.
**/
	function addSeries(chart, series, parseData) {
		var deferred = $.Deferred();
		
		if (!$.isArray(series)) {
			series = [series];
		}
		
		$.map(series, function (item) {
			var parsedSeries = null;
			
			// Allow any custom parsing supplied in `parseData` to be performed.
			if (typeof parseData === 'function') {
				item.data = parseData(item.data);
			}
			
			// Perform 'standard' parsing.
			if (item) {
				parsedSeries = parseSeries(chart, item);
			}
			
			// If this series has any data to show, add it to the chart (but
			// don't redraw the chart just yet).
			if (parsedSeries !== null) {
				chart.addSeries(parsedSeries, false);
			}
			
			// Return the parsed value for the mapping.
			return parsedSeries;
		});
		
		// Update the chart according to the data that is being displayed.
		if (isSeriesEmpty(chart, series)) {
			chart.showLoading('No data available');
			
			return deferred.reject();
		} else {
			chart.hideLoading();
			
			chart.redraw();
			
			return deferred.resolve();
		}
	}
	
/**
 * 
**/
	function isSeriesEmpty(chart, series) {
		var sum,
			seriesWithData = 0;
		
		if (series.length === 0 || series[0] == null) {
			return true; // Series has no data.
		} else if (chart.options.chart.type === 'pie') {
			if (series[0].data.length === 0) {
				return true; // Pie-chart with no data.
			}
			
			// Verify that the pie-chart has at least two non-zero points.
			sum = _.reduce(_.pluck(series[0].data, 'y'), function (memo, val) {
				if (val > 0) {
					seriesWithData++;
				}
				
				return memo + val;
			}, 0);
			
			if (sum === 0 || seriesWithData < 2) {
				return true; // Pie chart with only zero-value datapoints.
			} else {
				return false; // Pie-chat with some non-zero datapoints.
			}
		} else {
			return false; // Series has some data.
		}
	}
		
/**
 * Reduces the number of points in a chart, so that there are no more points
 * than pixels to display them.
 *
 * This is a hack - the server should handle this.
**/
	function parseSeries(chart, series) {
		var width = chart.plotSizeX,
			i,
			length = series.data.length,
			parsedData = [];
		
		if (length === 0) {
			return null;
		}
		
		if (length <= width) {
			return series;
		}
		
		for (i = 0; i < length; i += 10 * length / width) {
			parsedData.push(series.data[Math.floor(i)]);
		}
		
		series.data = parsedData;
		
		return series;
	}
	
/**
 * Toggles matching series for linked charts, matching by the series name, and
 * toggles the visibility state.
 * 
 * @param {Highcharts.Series|Highcharts.Point} clickedItem - The series or point
 * that was clicked to be toggled.
 * @param {Highcharts.Chart[]} linkedCharts - The linked charts to search for
 * matching points to toggle.
**/
	function toggleLinkedChartSeries(clickedItem, linkedCharts) {
		var itemName = clickedItem.name,
			visibility = !clickedItem.visible; // Toggle visibility state.
		
		$.each(linkedCharts, function (index, chart) {
			if (chart.options.chart.type === 'pie') {
				// Points in the linked pie-charts equate to series in other
				// charts, so we match the name by point name.
				$.each(chart.series[0].points, function (index, point) {
					if (point.name === itemName) {
						point.setVisible(visibility);
						
						// Also hide point tooltip, if visible.
						if (point.isTooltipDisplayed) {
							hidePoint(point);
						}
					}
				});
			} else {
				// Match series by name.
				$.each(chart.series, function (index, series) {
					if (series.name === itemName) {
						series.setVisible(visibility);
						
						// Hide any visible point tooltips for this series.
						hideDisplayedPoints(series);
					}
				});
			}
		});
	}
}));
