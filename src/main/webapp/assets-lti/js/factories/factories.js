'use strict';
/* global canvasSupportApp, errorDisplay, generateCurrentTimestamp  */

//COURSES FACTORY - does the request for the courses controller
canvasSupportApp.factory('Courses', function ($http) {
  return {
    getCourses: function (url) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          // Report Canvas errors
          if (result.data.errors) {
            errorDisplay(url, result.status, result.data.errors);
          }
          else {
            return result; 
          }
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get courses');
          return result;
        }
      );
    }
  };
});

//TERMS FACTORY - does the request for terms
canvasSupportApp.factory('Terms', function ($http) {
  return {
    getTerms: function (url) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          //forward the data - let the controller deal with it
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get terms');
          return result;
        }
      );
    }
  };
});

//COURSE FACTORY - does the request for the single course controller
canvasSupportApp.factory('Course', function ($http) {
  return {
    getCourse: function (url) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          // Report Canvas errors
          if (result.data.errors) {
            errorDisplay(url, result.status, result.data.errors[0].message);
          }
          return result; 
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get course');
          return result;
        }
      );
    },
    getMPathwaysCourses: function (url, sis_term_id) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          // MPathways returns valid data, but with errors (bad uniqname)
          if (result.data.Meta.httpStatus === 404) {
            errorDisplay(url, result.data.Meta.httpStatus, result.data.Result.ErrorResponse.responseDescription);
            //not a 404, and has a class listing
          } else if(result.data.Result.getInstrClassListResponse.InstructedClass) {
            return prepareMPathData(result.data, sis_term_id);  
          }
          //not a 404, and has no class listing, which is unlikely as they ARE in a course
          else {
            errorDisplay(url, result.data.Meta.httpStatus, 'MPathways reported no courses for you.');
          }
          
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get MPathways data');
          return result;
        }
      );
    },
    xListSection: function (url) {
      return $http.post(url).then(
        function success(result) {
          return result;  
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to cross list');
          return result;
        }
      );
    }
  };
});

//SECTIONS FACTORY - does the request for the sections controller
canvasSupportApp.factory('Sections', function ($http) {
  return {
    getSectionsForCourseId: function (courseId) {
      var url = '/canvasCourseManager/manager/api/v1/courses/' + courseId + '/sections?per_page=100&_='+ generateCurrentTimestamp();
      return $http.get(url, {cache: false}).then(
        function success(result) {
          if (result.data.errors) {
            errorDisplay(url, result.status, result.data.errors[0].message);
          }
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get sections');
          result.errors.failure = true;
        }
      );
    }
  };
});



//FRIEND LOOKUP FACTORY - does the requests for the friend controller
canvasSupportApp.factory('Friend', function ($http, $rootScope) {
  return {
    lookUpCanvasFriend: function (friendId) {
      var url = '/canvasCourseManager/manager/api/v1/accounts/self/users?search_term=' + friendId + '&_='+ generateCurrentTimestamp();
      return $http.get(url, {cache: false}).then(
        function success(result) {
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, result.data.errors);
          return result;
        }
      );
    },

    createCanvasFriend: function (friendEmailAddress,friendNameFirst, friendNameLast) {
      var url = '/canvasCourseManager/manager/api/v1/accounts/1/users?account_id=1' +
        '&user[name]=' + friendNameFirst + ' ' + friendNameLast +
        '&pseudonym[unique_id]=' + friendEmailAddress.replace('@','%2B') +
        '&pseudonym[sis_user_id]=' + friendEmailAddress +
        '&pseudonym[send_confirmation]=true' +
        '&communication_channel[type]=email' +
        '&communication_channel[address]=' + friendEmailAddress +
        '&communication_channel[skip_confirmation]=true' +
        '&force_validations=false';
      
      return $http.post(url).then(
        function success(result) {
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, result.data.errors);
          return result;
        }
      );
    },
    doFriendAccount: function (friendEmailAddress, requestorEmail, notifyInstructor, requestorFirst, requestorLast) {
      if(!notifyInstructor || notifyInstructor === 'true'){
        notifyInstructor = 'true';
      }
      else {
       notifyInstructor = 'false';
      }
      var url = '/canvasCourseManager/friend/friendCreate?id=' + friendEmailAddress +
       '&inst_email=' + requestorEmail +
       '&inst_first_name=' + requestorFirst +
       '&inst_last_name='  + requestorLast +
       '&notify_instructor='  + notifyInstructor;
       
      return $http.post(url, {cache: false}).then(
        function success(result) {
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to create friend');
          return result;
        }
      );
    },

    addFriendToSection: function (url) {
      return $http.post(url).then(
        function success(result) {
          return result;
        },
        function error(result) {
          console.log(result)
          //errorDisplay(url, result.status, 'Unable to create friend');
          return result;
        }
      );
    },
  };
});

canvasSupportApp.factory('focus', function($timeout, $window) {
  return function(id) {
    $timeout(function() {
      var element = $window.document.getElementById(id);
      if(element)
        element.focus();
    });
  };
});