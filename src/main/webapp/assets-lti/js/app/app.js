'use strict';
/* global $,  angular, getTermArray, _, getCurrentTerm, errorDisplay */

var canvasSupportApp = angular.module('canvasSupportApp', ['sectionsFilters','ui.sortable', 'ngAnimate']);

canvasSupportApp.run(function ($rootScope) {
	//for any init values needed
	$rootScope.server = "";
	$rootScope.user = {};
	// this would be determined from the LTI launch parameters - if 
	// a course, set to true, if not set to false
	// in singleViewMode only one course (the current course)can be expanded
	// 
	$rootScope.courseMode = true;

});

