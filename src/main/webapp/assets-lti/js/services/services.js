'use strict';
/* global  canvasSupportApp, FormData, angular,alert  */

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


canvasSupportApp.service('fileUpload', ['$http', function($http) {
  this.uploadFileAndFieldsToUrl = function(file, fields, uploadUrl) {
    var fd = new FormData();
    fd.append('file', file);
    angular.forEach(fields, function(value, key) {
      fd.append(key, value);
    });

    $http.post(uploadUrl, fd, {
        transformRequest: angular.identity,
        headers: {
          'Content-Type': undefined
        }
      })
      .success(function() {})
      .error(function() {
        alert('Sorry, I can\'t do this Dave');
      });
  };
}]);


canvasSupportApp.directive('fileModel', ['$parse', function($parse) {
  return {
    restrict: 'A',
    link: function(scope, element, attrs) {
      var model = $parse(attrs.fileModel);
      var modelSetter = model.assign;

      element.bind('change', function() {
        scope.$apply(function() {
          modelSetter(scope, element[0].files[0]);
        });
      });
    }
  };
}]);
