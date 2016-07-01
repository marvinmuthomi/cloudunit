/*
 * LICENCE : CloudUnit is available under the Affero Gnu Public License GPL V3 : https://www.gnu.org/licenses/agpl-3.0.html
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

(function () {
  'use strict';

  angular
    .module ( 'webuiApp.editApplication' )
    .directive ( 'editAppMonitoringApplication', MonitoringApp );

  function MonitoringApp () {
    return {
      restrict: 'E',
      templateUrl: 'scripts/components/editApplication/monitoring/application/editApplication.monitoring.application.html',
      scope: {
        app: '='
      },
      controller: [
        '$scope',
        'ApplicationService',
        '$stateParams',
        'MonitoringService',
        '$interval',
        MonitoringAppCtrl
      ],
      controllerAs: 'monitoringApp',
      bindToController: true
    };
  }

  function MonitoringAppCtrl ( $scope, ApplicationService, $stateParams, MonitoringService, $interval ) {

    // ------------------------------------------------------------------------
    // MONITORING
    // ------------------------------------------------------------------------

    var vm = this;
    vm.cleanFirstValue = true;
    vm.queueName = '';
    vm.queueStats = {};
    vm.selectedQueueStats = {};
    vm.displayGraph = [];
    vm.queueNameTab = [];
    vm.timer = {};
    var test = 60;
    
    vm.loadStats = loadStats;
    vm.deleteGraph = deleteGraph;
    vm.chooseQueue = chooseQueue;
    
    var lastQueueNameSelected = '';
    
    
    $scope.$on ( '$destroy', function () {
      $interval.cancel(vm.timer);
      vm.timer = null;
    } );
    
    
    function referenceQueue(queueName) {
      if(!vm.queueNameTab.includes(queueName)) {
         vm.queueNameTab.push(queueName);
      }
      lastQueueNameSelected = queueName;
    }
    
    function chooseQueue(queueName) {
      vm.cleanFirstValue = true;
      MonitoringService.chooseQueue(vm.app.location, queueName)
        .then(success)
        .catch(error);

      function success(response) {
        vm.queueStats = response.value;
        
        // clean
        referenceQueue(vm.queueName);
        vm.errorMsg = '';
        vm.queueName = '';
        refreshSelectedQueueStats();
        
        vm.timer = $interval ( function () {
          angular.forEach(vm.queueNameTab, function(queueName, key) {
            MonitoringService.chooseQueue(vm.app.location, queueName)
            .then(function(response) {
              angular.forEach(vm.displayGraph, function(value, key) {

                if(value.location == lastQueueNameSelected && vm.cleanFirstValue) {
                  vm.selectedQueueStats[value.id] = true;
                  vm.displayGraph[key].data.shift();
                }
                if(vm.displayGraph[key].location == queueName) {
                  
                  vm.displayGraph[key].data.push(
                    {
                      "date":new Date(response.timestamp*1000),
                      "value":response.value[value.id]
                    }
                  );
                  
                  if(vm.displayGraph[key].data.length >= test) {
                    vm.displayGraph[key].data.splice(0, 1);
                  }
                }
              });
              vm.cleanFirstValue = false;
            })
          });
        }, 3000 )
        vm.queueStatsPoll = MonitoringService.queueStats;
      }

      function error(response) {
        vm.errorMsg = response.data.message;
        return vm.errorMsg;
      }
    }
    
    function cleanAll() {
      vm.cleanFirstValue = true;
      angular.forEach(vm.displayGraph , function(value, index) {
        //delete all element already selected for the current queue
        vm.displayGraph[index].data = [];
      });
      
      vm.displayGraph = vm.displayGraph.filter(
        function(x) {
          return lastQueueNameSelected != x.location;           
        }
      );
    }
    
    function loadStats(selectedQueueStats) {
      
      cleanAll();

      setTimeout(function() {
        angular.forEach(selectedQueueStats, function(isQueueSelected, key) {
          if(isQueueSelected) {
            vm.displayGraph.push({
              data: [],
              title: lastQueueNameSelected + " " + key,
              id: key,
              location: lastQueueNameSelected,
              description: lastQueueNameSelected,
              x_accessor:'date',
              y_accessor:'value',
              area: false
            }); 
          }
        });   
      }, 0);
      
    }
   
   function refreshSelectedQueueStats() {
      vm.selectedQueueStats = {};
      var res = vm.displayGraph.filter(
        function(x) {
          return lastQueueNameSelected == x.location;           
        }
      );
      
      angular.forEach(res, function(graph, index) {
        console.log(graph.id);
        vm.selectedQueueStats[graph.id] = true;
      });
   }
   
   function deleteGraph(queueStatsName, queueLocation) {
     angular.forEach(vm.displayGraph, function(graph, index) {
       if(graph.id == queueStatsName && graph.location == queueLocation) {
        vm.displayGraph.splice(index, 1);    
       }
     });
     refreshSelectedQueueStats();
   }
   
  }
}) ();

