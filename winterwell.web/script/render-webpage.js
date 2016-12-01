#!/usr/bin/phantomjs
 // File: render-webpage.js
// Author: Joe Halliwell <joe@winterwell.com>, Jonathan Hussey, Daniel W
//
// Based on example code from https://github.com/ariya/phantomjs/blob/master/examples/rasterize.js
// This requires a relatively recent e.g. >=1.9 version of phantomjs
// TODO: Review and incorporate more ideas from https://github.com/highslide-software/highcharts.com/blob/master/exporting-server/phantomjs/highcharts-convert.js
/**
 * ## How to debug print styling ##
 * Open the reports page in chrome
 * Option 1:    Select pdf version- This file is called and report is generated
 * Option 2:    View the report as a web page
 * Instructions 1.  Right click and inspect element OR F12
 *              2.  Click 'Elements' then hit Esc
 *              3.  Should see 'Emulation' tab
 *              4.  Choose 'Screen' then scroll down and select CSS Media
 *              5.  You can control screen size to emulate the view port setting below (optional)
 *              6.  The layout should switch to Print version
 * Magic ---->  7.  You can apply css changes through the console or whichever you prefer
 *              8.  If you select print you will see what the report looks like in print format
 *              9.  To match the layout to the settings within this file apply the jquery
 *                  within the console window (refer to the debugging code at the base of the document)
 *              10. Finally if you paste the code into the console from the base of the document you will
 *                  be able to affect the print layout of the html based report. HURRAH!
 */

var page = require('webpage').create(),
    system = require('system'),
    address, output, size;

if (system.args.length < 3 || system.args.length > 20) {
    console.log('Usage: ' + system.args[0] + ' URL filename [paperwidth*paperheight|paperformat] [zoom]');
    console.log('  paper (pdf output) examples: "5in*7.5in", "10cm*20cm", "A4", "Letter"');
    console.log("Args length is: " + system.args.length);
	console.log('  zoom: 0.5, 1, 2, "default"');
    console.log('  footer: html to use as the footer');
    phantom.exit(1);
} else {
    address = system.args[1];
    output = system.args[2];

    //@jonathan - originally w:600
    //viewport size determines rendering
    //A4 pixel height size = 1123
    //1cm = 37.79px (38)    
    page.viewportSize = {
        //if you wish to avoid clipping on chart text then leave this (better implementation required)
        width: 1024, // this is to make bootstrap render as medium and a decen res
        height: 1448 // needed for page break calculations
    };

    // page.zoomFactor = 1.5;

    if (system.args.length > 3 && system.args[2].substr(-4) === ".pdf") {
        size = system.args[3].split('*');
        //in this case A4 is normally selected
        page.paperSize = size.length === 2 ? {
            width: size[0],
            height: size[1],
            margin: '0px',
            quality: '100'
        } : {
            format: 'A4',
            orientation: 'portrait',
            margin: {
                top: '1cm',
                left: '1cm',
                right: '1cm',
                bottom: '1cm'
            },
            quality: '100'
        };
    }

    if (system.args.length > 4 && parseFloat(system.args[4])>0) {
        page.zoomFactor = system.args[4];
    }

    var render_page = function() {
        console.log("Evaluating page");
        page.onConsoleMessage = function(msg) {
            console.log(msg);
        };

		// ??Would a local copy of jquery be faster?? Are there issues if the page already has jquery -- should we test for it before including??
        page.includeJs('https://cdn.jsdelivr.net/jquery/1.12.3/jquery.min.js', function() {
            page.evaluate(function() {
                // var $ = jQuery;

                //only do this if we are on the report page
                if (window.location.href.indexOf('/report') > -1) {

                    console.log("Reports being generated");

                    //Get the main css styles for sodash
                    var $links = $('link');
                    var $main, $print;
                    $.each($links, function(index, val) {
                        var $link = $(this);
                        if ($link.prop('href').indexOf('main.min.css')) {
                            $main = $(this);
                        } else if ($link.prop('href').indexOf('print.min.css')) {
                            $print = $(this);
                        }
                    });


                    // FIXME: Disable animations
                    // FIXME: The line below has no effect, presumably because it is happening too late!
                    // See https://github.com/highslide-software/highcharts.com/blob/master/exporting-server/phantomjs/highcharts-convert.js
                    Highcharts.SVGRenderer.prototype.Element.prototype.animate = Highcharts.SVGRenderer.prototype.Element.prototype.attr;

                    // Cull low opacity elements to workaround a bug with PhantomJS
                    // See http://stackoverflow.com/questions/16084405/exporting-highcharts-polar-chart-to-pdf-with-phantomjs
                    var paths = document.getElementsByTagName("path");
                    for (var i = paths.length - 1; i >= 0; i--) {
                        var path = paths[i];
                        var strokeOpacity = path.getAttribute('stroke-opacity');
                        if (strokeOpacity !== null && strokeOpacity < 0.2) path.parentNode.removeChild(path);
                    }
                    
                    //Opacity will cause webkit font rendering to render as an image
                    $('.TopImpact .message').css({
                        opacity: '1 !important',
                        border: '1px'
                    });

                    $('*').css({
                        opacity: '1 !important'
                    });

                    var $panels = $('.reportItem');

                    var panelHeight = 310;
                    var panelWidth = 500;


                    //we want to control the height of chart elements
                    //var chartHeight = 350;

                    for (var i = Highcharts.charts.length - 1; i >= 0; i--) {

                        var chart = Highcharts.charts[i];
                        var $renderTo = $(chart.renderTo);
                        var $container = $(chart.container);
                        var $panel = $container.closest('.panel');
                        var $panelbody = $($panel.find('.panel-body'));

                        $container.width(panelWidth - 15);
                        $container.height(panelHeight - 15);
                        $renderTo.width(panelWidth - 15);


                        if ($panel.find('.highcharts-container').length > 1) {
                            chart.setSize(panelWidth - 15, panelHeight - 15);
                        } else {
							chart.setSize(panelWidth - 15, panelHeight - 15);
                        }
                    }

                    $('.instagram-post.thumbnail').each(function() {
                    	$column = $(this).closest('*[class^="col"]')
                    	$column.width('200px');
                    	$column.css('display', 'inline-block');
                    });
                    
                    //our pdf height
                    var pdfHeight = 1631;
                    var padding = 38; // this is 1cm to match the page margins
                    var contentHeight = pdfHeight - (2 * padding);

                    //page one content                
                    var $logo = $('.print-header');
                    var $title = $('#page-header_container');
                    var pageOneInitialHeight = $logo.outerHeight(true) + $title.outerHeight(true);

                    // Set up page breaks
                    var currentPageHeight = pageOneInitialHeight;

                    $.each($panels, function(index, val) {
						// Get the height of the panel including the margin
                        var $panel = $(this);
                        var panelHeight = $panel.outerHeight(true);

                        if ((currentPageHeight + panelHeight) > pdfHeight) {
                        	// Start a new page...
                            $panel.css({
                                pageBreakBefore: 'always'
                            });
                            // ... and reset the content height.
                            currentPageHeight = panelHeight;
                        } else {
                            // Add to the current page's content height.
                            currentPageHeight = currentPageHeight + panelHeight;
                        }
                    });
                }
            });

            // Do the actual render
            console.log("Rendering to " + output);
            page.render(output);
            phantom.exit();
        });
    };

    page.open(address, function(status) {
        if (status !== 'success') {
            console.log('Unable to load the address!');
            phantom.exit();
        } else {
            console.log('Page loaded. Rendering.');
            // Wait a bit to let animations run
            window.setTimeout(render_page, 4000);
        }
    });

    // HACK: If all else fails generate something and exit cleanly...
    // FIXME: Not convinced this is the correct behaviour. Pro: robust in face of some errors; Con: Caller (Java) doesn't know things have gone awry
    window.setTimeout(function() {
        console.log('Timed out!');
        render_page();
    }, 20000);
}

/**
 * Your debugging code
 *
 * var documentHeight = $(document).height(),
    heightPerPage = 790,
    minimumDistanceFromTop = 200,
    pageCount = documentHeight / heightPerPage;
$('.report-section').each(function() {
    var offsetTopDocument = $(this).offset().top,
        offsetTopPage = offsetTopDocument % heightPerPage,
        elHeight = $(this).height(),
        pageNum = offsetTopDocument / heightPerPage,
        pageNumfloor = Math.floor(pageNum),
        boundary = (pageNumfloor+1)*heightPerPage,
        elPosition = offsetTopDocument + $(this).height();


    //basically offset top
    var availableSpace = Math.floor(heightPerPage - heightPerPage*(pageNum-pageNumfloor));

    console.log($(this).attr('class') + "\n" +
        "pageNum: " + pageNum + "\n" +
        "offsetTopPage: " + offsetTopPage + "\n" +
        "offsetTopDocument: " + offsetTopDocument + "\n" +
        "Element height: " + elHeight + "\n" +
        "Page: " + Math.floor(pageNum) + "\n" +
        "Boundary: " + boundary + "\n" +
        "Element Position: " + elPosition + "\n" +
        "-------------------------END OF SECTION----------------------------");

    if($(this).hasClass('WorldMap')){
        $(this).css('page-break-before', 'always');
    }else if(elPosition > boundary){
        $(this).css('page-break-before', 'always');
        $(this).css('page-break-after', 'auto');
    }else {
        $(this).css('page-break-before', 'auto');
    }

    //this does affect the page layout for printing
    // if(availableSpace<elHeight){
    //  $(this).css('page-break-before', 'always');
    // }


    if (pageNum >= 1 && offsetTopPage < minimumDistanceFromTop) {
        // $(this).css('page-break-before', 'always');
        console.log('-----------add page break--------------');
    }
})

if ((pageCount - Math.floor(pageCount)) > 0) {
    pageCount = Math.floor(pageCount) + 1;
}

console.log(pageCount);
 */

/**
 * This will output the following sample
 *
 * traffic-over-time report-section
 * pageNum: 0.21265822784810126
 * offsetTopPage: 168
 * offsetTopDocument: 168
 * Element height: 848
 * Page: 0
 * Boundary: 790
 * Element Position: 1016
 * -------------------------END OF SECTION----------------------------
 * time-of-day report-section
 * pageNum: 1.3113924050632912
 * offsetTopPage: 246
 * offsetTopDocument: 1036
 * Element height: 448
 * Page: 1
 * Boundary: 1580
 * Element Position: 1484
 */
