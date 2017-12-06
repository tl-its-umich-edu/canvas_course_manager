'use strict';

/* global canvasSupportApp, errorDisplay, generateCurrentTimestamp */

canvasSupportApp.factory('Courses', function ($http, $q) {
  var getCourses = function(url) {
    var courses = [];
    var deferred = $q.defer();
    var getNext = function(url) {
      $http.get(url)
        .then(function(result) {
          courses = courses.concat(result.data);
          if (result.headers('Next')) {
            getNext(decodeURIComponent(result.headers('Next')));
          } else {
            result.data = courses;
            deferred.resolve(result);
          }
        }, function(result) {
          errorDisplay(url, result.status, 'Unable to get courses');
            deferred.resolve(result);
        });
    };
    getNext(url);

    return deferred.promise;

  };

  return {
    getCourses: function(url) {
      return getCourses(url);
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
    getGroups: function (url) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          // Report Canvas errors
          if (result.data.errors) {
            errorDisplay(url, result.status, result.data.errors[0].message);
          }
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get groups');
          return result;
        }
      );
    },
    postGroupSet: function (url) {
      return $http.post(url).then(
        function success(result) {
          // Report Canvas errors
          if (result.data.errors) {
            errorDisplay(url, result.status, result.data.errors[0].message);
          }
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to create group set');
          return result;
        }
      );
    },
    getMPathwaysCourses: function (url, sis_term_id) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          //ESB down, timed out
          if(result === undefined){
            return {"Meta":{"Message":"COMPLETED","httpStatus":666},"Result":{"ErrorResponse":{"responseDescription":"Server not available","responseCode":666}}}
          }
          //Malformed request (because sis_course_id is bogus)
          if (result.data.Meta.httpStatus === 404) {
            return [];
          }
          //Malformed request (because sis_course_id is bogus)
          if (result.data.Meta.httpStatus === 400) {
            return [];
          }
          //MPath not available
          if (result.data.Meta.httpStatus === 666) {
            return result;
          }
          // ESB down
          if (result.data.Meta.httpStatus === 503) {
            return result;
          }

          if(result.data.Result.getInstrClassListResponse.InstructedClass) {
            return prepareMPathData(result.data, sis_term_id);
          }
          else {
            return [];
          }

        },
        function error(result) {
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
//Handling the LTI request different to mask the API call
canvasSupportApp.factory('Sections', function ($http) {
  return {
    getSectionsForCourseId: function (courseId, context) {
    	if ( context === true){
        //REGEXINFO: canvas.api.getallsections.per.course.regex
    		//TODO: modify per page to default to 100 using property
    		var url = '/canvasCourseManager/manager/api/v1/courses/course_id/sections?per_page=100&_='+ generateCurrentTimestamp();
    	}
    	else{
        //REGEXINFO: canvas.api.getallsections.per.course.regex
    	//TODO: modify per page to default to 100 using property
    		var url = '/canvasCourseManager/manager/api/v1/courses/' + courseId + '/sections?per_page=100&_='+ generateCurrentTimestamp();
    	}
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
      //REGEXINFO: canvas.api.search.user.regex
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
     //REGEXINFO: canvas.api.create.user.regex
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
          errorDisplayFriendCreateToCanvas(url, result.status, 'UM Course Manager cannot add \''+friendEmailAddress
                +'\' to Canvas. Try adding them with the +People button in the People tool. If that doesn\'t work, please contact 4help@umich.edu');
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

    addFriendToSection: function (url, sectionName, sectionNumber) {
      return $http.post(url).then(
        function success(result) {
          var resultSection = [];
          resultSection.push({'section_name':sectionName,'section_number':sectionNumber});
          resultSection.push(result.data);
          result.data = resultSection;
          return result;
        },
        function error(result) {
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
//GENERIC GETTER of things
canvasSupportApp.factory('Things', function ($http, $q) {
  var getThings = function(url) {
    var things = [];
    var deferred = $q.defer();
    var getNext = function(url) {
      $http.get(url)
        .then(function(result) {
          things = things.concat(result.data);
          if (result.headers('Next')) {
            getNext(result.headers('Next'));
          } else {
            result.data = things;
            deferred.resolve(result);
          }
        }, function(result) {
          errorDisplay(url, result.status, 'Unable to get things');
            deferred.resolve(result);
        });
    };
    getNext(url);
    return deferred.promise;
  };
  return {
    getThings: function(url) {
      return getThings(url);
    }
  };
});
