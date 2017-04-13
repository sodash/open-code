/**
 * Renders charts marked with the class 'renderWithFlot'. Currently, data is
 * stored in the text of the element; this should be moved into the relevant
 * attributes.
 * 
 * @requires jQuery
 * @requires Charts.js
 * @author Steven King <steven@winterwell.com>
**/
$(function () {
	var masterCharts = {},
		slaveCharts = {};
	
	$('.renderWithFlot').not('.ajaxed').each(function () {
		var type, // = $(this).attr('data-chart-type'),
			data = $(this).attr('data-chart-data'),
			options = $(this).attr('data-chart-options');
		
		$(this).addClass('ajaxed');
		
		if (data) {
			data = JSON.parse(data);
		}
		
		if (options) {
			options = JSON.parse(options);
		}
		
		// TODO Move to attribute (data-chart-type).
		if (!type) {
			if ($(this).hasClass('pie')) {
				type = 'pie';
			} else if ($(this).hasClass('timeseries')) {
				type = 'timeseries';
			} else if ($(this).hasClass('timedist')) {
				type = 'timedist';
			} else if ($(this).hasClass('time-of-day')) {
				delete options.xaxis.ticks; // This enables us to format the ticks correctly in Charts.js.
				
				type = 'timeofday';
			}
		}
		
		if ($(this).hasClass('chart-master')) {
			var masterChartId = $(this).prop('id');
			
			$(this).after('<div id="legend-' + masterChartId + '" class="chart-legend-external chart-legend-shared">');
			
			masterCharts[masterChartId] = {
				$el: $(this),
				type: type,
				data: data,
				options: options
			};
		} else if ($(this).attr('data-chart-master-id')) {
			var masterChartId = $(this).attr('data-chart-master-id');
			
			$('#' + masterChartId).after($(this));
			
			slaveCharts[masterChartId] = slaveCharts[masterChartId] || [];
			
			slaveCharts[masterChartId].push({
				$el: $(this),
				type: type,
				data: data,
				options: options
			});
		} else {
			charts.render({
				$el: $(this),
				type: type,
				data: data,
				options: options
			});
		}
	});
	
	for (var masterChartId in masterCharts) {
		charts.renderLinked({
			master: masterCharts[masterChartId],
			slaves: slaveCharts[masterChartId],
			$legend: $('#legend-' + masterChartId)
		});
	}
	
	// wkhtmltopdf trigger. This is required otherwise time isn't given to render graphs.
	window.status = 'FLOT_DONE';
});
