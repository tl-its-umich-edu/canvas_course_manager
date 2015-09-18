'use strict';
/* global  canvasSupportApp  */

canvasSupportApp.service('SectionSet', function($rootScope) {
  var sectionSet = [];

  var setSectionSet = function(sourceSections) {
      sectionSet = [];
      sectionSet.push(sourceSections);
      $rootScope.$broadcast('courseSetChanged', sectionSet);
  };

  var getSectionSet = function(){
      return sectionSet;
  };

  return {
    setSectionSet: setSectionSet,
    getSectionSet: getSectionSet
  };

});