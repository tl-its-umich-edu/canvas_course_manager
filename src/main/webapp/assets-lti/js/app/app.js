'use strict';
/* global angular*/

var canvasSupportApp = angular.module('canvasSupportApp', ['sectionsFilters', 'ngAnimate', 'ngAria','ngRoute']);

canvasSupportApp.run(function($rootScope) {
  //for any init values needed
  $rootScope.server = '';
  $rootScope.user = {};
  // we are setting the throttling to 200k row limit for the admin tool.
  //Todo this needs to be deleted and set in an external property
  $rootScope.csv_throttle = 200000;
  //to turn on/off the 2 functionalities
  $rootScope.functionality = {
    'friends': true,
    'sections': true
  };
  // this uses an object populated with LTI launch parameters and added to index.lti.vm
  $rootScope.ltiLaunch = ltiLaunch;
});
// setting up routes to the different templategs and controllers
canvasSupportApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
    when('/', {
      templateUrl: 'assets-lti/views/course.html',
      controller: 'courseController'
    }).
    when('/grades', {
      templateUrl: 'assets-lti/views/grades.html',
      controller: 'gradesController'
    }).
    when('/saa', {
      templateUrl: 'assets-lti/views/saa.html',
      controller: 'saaController'
    }).
    otherwise({
      redirectTo: '/'
    });
  }
]);
