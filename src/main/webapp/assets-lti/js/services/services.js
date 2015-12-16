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


canvasSupportApp.directive('eventFocus', function(focus) {
  return function(scope, elem, attr) {
    elem.on(attr.eventFocus, function() {
      focus(attr.eventFocusId);
    });
    scope.$on('$destroy', function() {
      elem.off(attr.eventFocus);
    });
  };
});