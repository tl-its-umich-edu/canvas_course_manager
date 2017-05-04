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

//directive to pass the focus on to a specific the model item
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

// directive to upload a file
canvasSupportApp.service('fileUpload', ['$http','$log', function($http, $log) {
  this.uploadFileAndFieldsToUrl = function(file, uploadUrl, doneCallBack) {
    var fd = new FormData();
    fd.append('file', file);
    $http.post(uploadUrl, fd, {
        transformRequest: angular.identity,
        headers: {
          'Content-Type': undefined
        }
        //success
      }).then (function(response) {
        doneCallBack(response);
        //failure
      },function(response) {
            doneCallBack(response);
        });
  };
}]);

//directive to read a file in a file[type:input]
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
