/* 
 * Copyright 2013 Corpuslinguistic working group Humboldt University Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

window.annis_gui_components_ScatterplotWhiteboard = function() {
  var div = this.getElement();
  var theThis = this;
  
  var lastValues = null;
  var lastFontFamily = "sans-serif";
  var lastFontSize = 10.0;
  var lastDateResolution = null;

  this.date2String = function(date, dateResolution) {
    if(dateResolution === 'years') {
      return moment(date).format("YYYY");
    } else if (dateResolution === 'month') {
      return moment(date).format("YYYY-MM");
    } else if(dateResolution === 'days') {
      return moment(date).format("YYYY-MM-DD");
    } else {
      return moment(date).format("YYYY-MM-DD");
    }
  };
  
  this.showData = function(values, fontFamily, fontSize, dateResolution) {    
    if(!values )
    {
      alert("invalid call to showData");
      return;
    }
    
    var dataSeries = [];
    
    for(var key in values)
    {
      var timeSeries = values[key];
      var d = [];
      for(var t in timeSeries) {
        d.push([Date.parse(t), timeSeries[t]]);
      }
       dataSeries.push({
         data: d,
         label: key
       });
    }
    
    // calculate our own ticks since the flotr way of doing it is broken for historical dates
    var xTicks = []
    var startDate = moment(d[0][0]).startOf(dateResolution);
    var endDate = moment(d[d.length-1][0]).endOf(dateResolution);
    var diffDate = endDate.diff(startDate, dateResolution);
    var tickSteps = diffDate / 8; // display at least 8 ticks + start and end
    xTicks.push(startDate.valueOf());
    
    var tick = startDate.clone();
    while(tick.isBefore(endDate)) {
      xTicks.push(tick.valueOf());
      tick.add(tickSteps, dateResolution);
    }
    xTicks.push(endDate.valueOf());
    
    
    $(div).remove("canvas");
    
    var graph = Flotr.draw(
      div,
      dataSeries,
      {
        lines: {
            show: true
        },
        points: {
          show: true
        },
        yaxis : {
        },
        xaxis : {
          ticks: xTicks,
          labelsAngle: 45,
          tickFormatter: function(v){
            var t = new Date(1*v);
            return theThis.date2String(t, dateResolution);
          }
        },
        mouse : {
          track : true,
          relative : true,
          trackFormatter: function(val) {
            var t = new Date(1*val.x);
            return theThis.date2String(t, dateResolution) + " - " + val.series.label + " (" + parseInt(val.y) + ")";
          }
        },
        legend: {
            position: 'se'  
        },
        HtmlText : false,
        fontSize : fontSize,
        fontFamily: fontFamily
      }
    );
        
    // bind click event
    graph.observe(div, 'flotr:click', function (position) {
      if(position.hit) {
        theThis.selectRow(position.hit.series.label, new Date(position.hit.x).toISOString());
      }
    }); 
    
    lastValues = values;
    lastFontFamily = fontFamily;
    lastFontSize = fontSize;
    lastDateResolution = dateResolution;
    
  };
  
  
  this.onStateChange = function() { 
    if(lastValues) {
      theThis.showData(lastValues, lastFontFamily, lastFontSize, lastDateResolution);
    }
  };
  
  // always resize the canvas to the size of the parent div
  $(window).resize(function() {
    if(lastValues) {
      theThis.showData(lastValues, lastFontFamily, lastFontSize, lastDateResolution);
    }
  });
};