# Winterwell Charting v2.0

Status: Conceptual
Authors: Steven, Daniel

## Requirements

### Core Features

 - Line chart (which can display several lines)
 	- Info-popups for points, which can be pinned open
 - Pie chart
 - Bar chart / histogram
 - Legend:
 	- Interactive: click to show/hide a component
 - Ability to link a line-chart, pie-chart and table together, so that show/hide affects them. 
 - Colour control: both manually-set-colours and auto-set-colours
 - Control width & height
 - Axis labels
 	- Support for using unix timestamps on the x-axis
 	- Support for discrete x-axis (e.g. for a bar-chart where the x-axis is tags-in-a-tagset)
    - Sensible axis scaling
    - Sensible tics & tic labels (especially for datetimes)
    - Support for local time vs gmt
 - Looks good!
 
#### Architectural

 - Can be used from Java (via html & wkhtmltopdf or phantomjs)
 - Can load & display data by ajax, or can include the data within it's html.
 - Prints out (can be via render-to-pdf or other mechanism)
 - Can have several charts on one page without them interfering with each other.
 - A library rather than a framework (i.e. can play nicely with other code).
 - Clear dependencies: 
   - Java: winterwell.utils, winterwell.web, & winterwell.maths
   - Javascript: jQuery?? underscore?? others??
 - Works (okay) on IE7, iPad and mobile (plus IE8, IE9 and proper browsers)

### Bonus Features

 - Pie-chart supports sub-slices (e.g. use-case: breakdown by device, where iPhone < mobile).
 - Line-chart supports min/max and confidence intervals (either using candlesticks, or (preferably) by shading regions).
 - Shading regions on a line chart
 - Customisable time format (though we'll settle for sensible i.e. not full RC3339)
 - Zoom in/out
 - Export: csv, Excel, png
 - Histogram supports variable width bars.
 - Legend can include extra info about an item (e.g. the dashboard chart shows "Source-name (total)")
 - Graceful handling of data-is-loading, data-loading-error, and no-data.
 - Sparklines (mini single-line charts)
 - Info-popups can use html (e.g. to include links or an image).
 - Auto-update of chart with realtime data. 
 - Automated tests
 - Per-series axes i.e. for when you want to chart linked series with very different units.
 - Chart drawing animations
 - Maps (chloropleth / heatmap) -- but these are probably worth a separate library, as they're likely to be quite different
and have map-specific dependencies.

## Specification ##

### Java interface ###

The Java API is designed to be framework-agnostic with respect to the plotting
library used to generate the chart, while also providing the flexibility to make
precise changes to the rendered chart, by passing an options map in the format
that is native to the charting library.[^1]

[^1]: It's worth noting that utilising this feature will create a tight-coupling
between the chart being rendered and the library used to perform the rendering,
so it should only be used with caution.

#### Chart and chart subclasses ####

Charts are defined by the `Chart` class, or one of its subclasses. The subclass
ofchart defines the type of chart and the axes that are set by default, but these
can be overridden if required. For example, a `TimseriesChart` would set the type
to 'line', and the x-axis type to 'datetime'.

The chart object provides methods to set the series' to plot, as well as allowing
for additional axes to be provided.

#### Chart series ####

The `ChartSeries` class contains a single data series to plot on a chart, plus
any specific options related to that series.

#### Chart axes ####

The `Axis` class provides methods to specify how a single axis should display.

#### Rendering the chart ####

A chart will provide the following rendering methods:

  - `String toJSON()` creates a JSON-encoded string-representation of a chart,
    that can be used by a front-end rendering library to plot the chart.
    
  - `void toImage(File outputFile, ImageType type)` creates an image of the
    plotted chart, and saves it to the supplied file.
    
  - `void toHtmlPage(File outputFile, IRenderLibrary)` creates a complete HTML
    page containing the rendered chart, using the supplied rendering library. The
    page contains all required dependencies.
    
  - `String toHtml()` creates the HTML required to generate the chart in an HTML
    page. The library used to render the chart, and its dependencies, must be 
    supplied elsewhere.
    
  - `String toHtml(String elementId)` creates the HTML required to generate the
    chart in an HTML page. In this case, the HTML element in which the chart is 
    to be rendered is assumed to exist on the page, and will not be created. The
    library used to render the chart, and its dependencies, must be supplied as
    well.

### JavaScript interface ###

A front-end chart rendering library is wrapped to provide a standard interface
that adheres to the above data structure. It provides the method:

    Chart.render(chart)

where `chart` contains an object or JSON-encoded string adhering to the above
structure. If an array of charts is provided, the charts will be linked, so that
they share the legend of the first chart.

### Chart data structure ###

The chart data structure defines the contract between the back- and front-end
parts of the chart rendering process. For typical java-based useage, it would be
assembled by one of the chart render methods on a `Chart` subclass. So it is
primarily included here for reference.

The data structure for a chart consists of the following fields:

  - el: The id of the HTML element in which the chart should be rendered. It
    should be assumed that any existing content will be overwritten.
        
  - type: The type of chart to render. This allows the front-end library to use
    the best default options when creating the chart. If not supplied, the
    default options specified by the library will be used.
    
    Supported types are: 'line', 'area', 'column', 'bar', 'scatter',
    and 'pie'.
    
  - title: The title to display on the chart.
    
  - subtitle The subtitle to display on the chart.
    
  - series: An array of series to render to the chart. Can contain the fields:
    
      - name A name used to identify the series. Used in the legend, if one is
        visible.
      - data See below for details.
      - type To plot multiple types of plot on one chart, the type can also be
        specified per-series.
      - xAxis The id of the x-axis to use, if multiple are present.
      - yAxis The id of the y-axis to use, if multiple are present.
      - color Specifies a color to use for this series, in the form '#rrggbb'.
    
    Alternatively, a string can be provided, that points to a url that provides
    the data to render, in the same format as above.
    
  - xaxis: Data regarding the x-axis. Can be an array for multiple axes. Can
    contain the fields:
    
      - id A unique id for this axis, if multiple are present.
      - title The label to display for an axis.
      - type Can be one of 'linear', 'logarithmic', or 'datetime'.
      - min Used to fix the minimum value of an axis.
      - max Used to fix the maximum value of an axis.
      - categories An array of discrete labels, to be used in place of numerical
        values.
    
  - yaxis: Data regarding the y-axis. Can be an array for multiple axes. Fields
    are the same as for the x-axis.
    
  - legend: Allows configuration of the chart legend. Can contain the fields:
    
      - title A title to display on the legend.
      - align The horizontal position of the legend, can be one of 'left',
        'center', or 'right'.
      - verticalAlign The vertical position of the legend, can be one of 'top',
        'middle', 'bottom'.
    
  - options: A map of options to extend those that are selected by default. Must
    be formatted for the selected front-end library. This allows for the precise
    modification of the rendered chart from the back-end.

The data for a series can be supplied in one of three formats:

 1. An array of numerical values. Each value is interpreted as the y-axis value,
    with the x-axis values being assigned incrementally, or generated from the
    plot options. For example:
    
        [3, 1, 4, 2]
    
 2. An array of arrays. Each array should consist of a pair of numbers, that are
    the x- and y-values for each data point.
    
        [[0, 3], [1, 1], [2, 4], [3, 2]]
    
 3. An array of objects. Each object can supply configuration options for each
    point, such as the name or colour.
    
        [{
            "x": 0, "y": 3, "color": "black"
        }, {
            "x": 1, "y": 1, "color": "red"
        }, {
            "x": 2, "y": 4, "color": "green"
        }, {
            "x": 3, "y": 2, "color": "blue"
        }]
    
    Each point object can contain the following fields:
    
      - name A name for the point. In a linked-chart, if the name of a point in
        a pie-chart is the same as the name of a series, then those two will be
        linked.
      - x The position of the point along the x-axis.
      - y The position of the point along the y-axis.
      - color The colour that the point should be rendered in.
      - label A label to display beside the point.
