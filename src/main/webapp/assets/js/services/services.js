'use strict';
/* global  canvasSupportApp  */

/*

 canvasSupportApp.service('SectionSet', function() {
  SectionSet
  return {
    setSectionSet: function(sourceSections) {
      sectionSet = sourceSections;
    },
    getSectionSet: function() {
      return sectionSet;
    },
  };
});

canvasSupportApp.service('SectionSet', function() {
  var sectionSet = {};
  this.setSectionSet = function(sourceSections) {
    sectionSet = sourceSections;
  };
  this.getSectionSet = function() {
    sectionSet = sourceSections;
  };
});

canvasSupportApp.service('SectionSet', function() {
  var sectionSet = {};
  return {
    setSectionSet: function(sourceSections) {
      sectionSet = sourceSections;
      //$rootScope.$broadcast('XChanged', sectionSet);
    },
    getSectionSet: function() {
      return sectionSet;
       
    },
  };
});
*/
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