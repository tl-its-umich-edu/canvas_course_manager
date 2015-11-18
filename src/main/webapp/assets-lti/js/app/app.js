'use strict';
/* global angular*/

var canvasSupportApp = angular.module('canvasSupportApp', ['sectionsFilters','ui.sortable', 'ngAnimate']);

canvasSupportApp.run(function ($rootScope) {
	//for any init values needed
	$rootScope.server = '';
	$rootScope.user = {};
	//to turn on/off the 2 functionalities
	$rootScope.functionality = {
    'friends': true,
    'sections': true
	};
	// this uses an object populated with LTI launch parameters and added to index.lti.vm
	$rootScope.ltiLaunch = ltiLaunch;
});

