'use strict';
/* global  canvasSupportApp, errorDisplay, generateCurrentTimestamp  */

//COURSES FACTORY - does the request for the courses controller
canvasSupportApp.factory('Courses', function ($http) {
  return {
    getCourses: function (url) {
      return $http.get(url, {cache: false}).then(
        function success(result) {
          //forward the data - let the controller deal with it
          return result; 
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get courses');
          result.errors.failure = true;    
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
      var url = '/sectionsUtilityTool/manager/api/v1/courses/' + courseId + '/sections?per_page=100&_='+ generateCurrentTimestamp();
      return $http.get(url, {cache: false}).then(
        function success(result) {
          return result;
        },
        function error(result) {
          errorDisplay(url, result.status, 'Unable to get sections');
        }
      );
    }
  };
});

//FRIEND LOOKUP FACTORY - does the requests for the friend controller
canvasSupportApp.factory('Friend', function ($http, $rootScope) {
  return {
    lookUpCanvasFriend: function (friendId) {
      var url = '/sectionsUtilityTool/manager/api/v1/accounts/self/users?search_term=' + friendId + '&_='+ generateCurrentTimestamp();
      return $http.get(url, {cache: false}).then(
        function success(result) {
          return result;
        },
        function error() {
          // TODO: report error
        }
      );
    },

    createCanvasFriend: function (friendEmailAddress,friendNameFirst, friendNameLast) {
      var url = '/sectionsUtilityTool/manager/api/v1/accounts/1/users?account_id=1' +
        '&user[name]=' + friendNameFirst + ' ' + friendNameLast +
        //'&user[sortable_name]=' +  friendNameLast + ',' +  friendNameFirst +
        '&pseudonym[unique_id]=' + friendEmailAddress.replace('@','%2B') +
        '&pseudonym[sis_user_id]=' + friendEmailAddress +
        '&pseudonym[send_confirmation]=false' +
        '&communication_channel[type]=email' +
        '&communication_channel[address]=' + friendEmailAddress +
        '&communication_channel[skip_confirmation]=false';
      
      return $http.post(url).then(
        function success(result) {
          return result;
        },
        function error() {
          // TODO: report error
        }
      );
    },
    doFriendAccount: function (friendEmailAddress, requestorEmail) {
      var url = '/sectionsUtilityTool/friend/friendCreate?id=' + friendEmailAddress +
       '&inst_email=' + requestorEmail +
       // need the first name and last name, right now just using the email
       '&inst_first_name=' + requestorEmail +
       '&inst_last_name= ';
      return $http.post(url, {cache: false}).then(
        function success(result) {
          return result;
        },
        function error() {
          //TODO: report error
        }
      );
    },

    addFriendToSection: function (url) {
      return $http.post(url).then(
        function success(result) {
          return result;
        },
        function error() {
          // TODO: report error
        }
      );
    },




  };
});