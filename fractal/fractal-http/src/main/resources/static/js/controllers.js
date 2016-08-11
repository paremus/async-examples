'use strict';

/* Controllers */

function FractalDemoCtl($scope, $http, $timeout) {
  
  $scope.minX = null;
  $scope.maxX = null;
  $scope.minY = null;
  $scope.maxY = null;
  $scope.iterations = null;
  $scope.equation = null;
  $scope.colourScheme = null;
  $scope.async = false;
  
  $scope.updateDefaults = function() {
    $scope.minX = Number($scope.equation.minX);
    $scope.maxX = Number($scope.equation.maxX);
    $scope.minY = Number($scope.equation.minY);
    $scope.maxY = Number($scope.equation.maxY);
    
    $scope.iterations = Number($scope.equation.iterations);
  };

  $scope.render = function() {
    if($scope.eventSource) {
      $scope.eventSource.close();
    }
    
    var connString = "rest/render";

    connString = connString + "/" + ($scope.async ? "async" : "sync");
    
    connString = connString + "/" + $scope.minX + "/" + $scope.maxX + "/" + $scope.minY + "/" + $scope.maxY + "/" + 
          $scope.iterations + "/" + $scope.equation["equation.type"] + "/" + $scope.colourScheme;
    
    $scope.eventSource = new EventSource(connString);
    
    $scope.eventSource.onmessage = function(event) {
      event = JSON.parse(event.data);
      if(event.terminate) {
        $scope.eventSource.close();
        $scope.eventSource = null;
        return;
      }
    
      var ctx = document.getElementById('fractalCanvas').getContext('2d');
      
      for(var i = 0; i < event.data.length; i++) {
        for(var j = 0; j < event.data[i].length; j++) {
          ctx.fillStyle = event.colours[event.data[i][j]];
          ctx.fillRect(event.x + i, event.y + j, 1, 1);
        }
      }
    };
    return;
  };
  
  $scope.zoom = function(event) {
    var rect = document.getElementById('fractalCanvas').getBoundingClientRect();
  
    var range = $scope.maxX - $scope.minX;
    
    var xPixel = event.clientX - rect.left;
    var x = $scope.minX + xPixel * range / rect.width;
    
    $scope.minX = x - (range / 4);
    $scope.maxX = x + (range / 4);
    
    range = $scope.maxY - $scope.minY;

    var yPixel = event.clientY - rect.top;
    var y = $scope.maxY - yPixel * range / rect.height;
    
    $scope.minY = y - (range / 4);
    $scope.maxY = y + (range / 4);
    
    $scope.render();
  }
  
  $http.get('rest/config').success(function(data) {
    $scope.equations = data.equations;
    if(!$scope.equation) {
      $scope.equation = data.equations[0];
      $scope.updateDefaults();
    }
    
    $scope.colourSchemes = data.colourSchemes;
    if(!$scope.colourScheme) $scope.colourScheme = data.colourSchemes[0];
    
    if(!$scope.painted && $scope.colourSchemes && $scope.equation) {
      $scope.painted = true;
      $scope.render();
    }
  });
}
