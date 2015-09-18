'use strict';
/* global $,  angular, getTermArray, _, getCurrentTerm, errorDisplay */

var canvasSupportApp = angular.module('canvasSupportApp', ['sectionsFilters','ui.sortable', 'ngAnimate']);

canvasSupportApp.run(function ($rootScope) {
	//for any init values needed
	$rootScope.server = "";
	$rootScope.user = {};
});

