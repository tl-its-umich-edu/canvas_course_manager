'use strict';
/* global $, canvasSupportApp, getTermArray, _, getCurrentTerm, errorDisplay, validateUniqname, generateCurrentTimestamp */

/* TERMS CONTROLLER */
canvasSupportApp.controller('termsController', ['Courses', '$rootScope', '$scope', '$http', function (Courses, $rootScope, $scope, $http) {
  //void the currently selected term
  $scope.selectedTerm = null;
  //reset term scope
  $scope.terms = [];
  var termsUrl ='manager/api/v1/accounts/1/terms?per_page=4000&_=' + generateCurrentTimestamp();
  $http.get(termsUrl).success(function (data) {
    if(data.enrollment_terms){
      $scope.terms = data.enrollment_terms;
      $scope.$parent.currentTerm =  getCurrentTerm(data.enrollment_terms);
    }
    else {
      errorDisplay(termsUrl,status,'Unable to get terms data');  
    }
  }).error(function () {
    errorDisplay(termsUrl,status,'Unable to get terms data');
  });

  //user selects a term from the dropdown that has been 
  //populated by $scope.terms 
  $scope.getTerm = function (termId, termName, termCanvasId) {
    $scope.$parent.currentTerm.currentTermName = termName;
    $scope.$parent.currentTerm.currentTermId = termId;
    $scope.$parent.currentTerm.currentTermCanvasId = termCanvasId;
  };

}]);


//COURSES CONTROLLER
canvasSupportApp.controller('coursesController', ['Courses', 'Sections', '$rootScope', '$scope', 'SectionSet', function (Courses, Sections, $rootScope, $scope, SectionSet) {

 $scope.getCoursesForUniqname = function () {
    var uniqname = $.trim($('#uniqname').val().toLowerCase());
    $scope.uniqname = uniqname;
    var mini='/manager/api/v1/courses?as_user_id=sis_login_id:' +uniqname+ '&per_page=200&published=true&with_enrollments=true&enrollment_type=teacher&_='+ generateCurrentTimestamp();
    var url = '/sectionsUtilityTool'+mini;
    $scope.loading = true;
    if (validateUniqname(uniqname)) {
      Courses.getCourses(url).then(function (result) {
        if (result.data.errors) {
          // the call to CAPI has returned a json with an error node
          if(uniqname) {
            // if the uniqname field had a value, report the problem (bad uniqname)
            $scope.errorMessage = result.data.errors + ' ' + uniqname;
            $scope.errorLookup = true;
          }
          else {
            // if the uniqname field had no value ask for it
            $scope.errorMessage = 'Please supply a uniqname at left.';
            $scope.instructions = false;
            $scope.errorLookup = false;
          }
          // various error flags in the scope  that do things in the UI
          $scope.success = false;
          $scope.error = true;
          $scope.instructions = false;
          $scope.loading = false;
        }
        else {
          if(result.errors){
            // catch all error
            $scope.success = false;
            $scope.error = true;
            $scope.instructions = false;
            $scope.loading = false;
          }
          else {
            // all is well - add the courses to the scope, extract the terms represented in course data
            // change scope flags and get the root server from the courses feed (!)
            $scope.courses = result.data;
            $scope.termArray = getTermArray(result.data);
            $scope.error = false;
            $scope.success = true;
            $scope.instructions = true;
            $scope.errorLookup = false;
            $scope.loading = false;
            $rootScope.server = result.data[0].calendar.ics.split('/feed')[0];
            $rootScope.user.uniqname = uniqname;
          }
        }
      });
    }
    else {
      $scope.loading = false;
      $('#uniqnameValidMessage').show();
    }
  };
  // make the sections sortable drag and droppable the angular way
  $scope.sortableOptions = {
      placeholder: 'section',
      connectWith: '.sectionList',
      start: function(event, ui) {
        ui.item.addClass('grabbing');
      },
      receive: function(event, ui) {
      //on drop, append the name of the source course
        var prevMovEl = ui.item.find('.status');
        if(prevMovEl.text() !==''){
          prevMovEl.next('span').show();
        }
        if(ui.sender.closest('.course').find('.section').length ===1){
          ui.sender.closest('.course').addClass('onlyOneSection');
        }
        if(ui.sender.closest('.course').find('.section').length ===0){
          ui.sender.closest('.course').addClass('noSections');
        }
        prevMovEl.text('Moved  from ' + ui.sender.closest('.course').find('.courseLink').text());
        $('li.course').removeClass('activeCourse');
        ui.item.closest('li.course').addClass('activeCourse');
      },
      stop: function( event, ui ) {
        //add some animation feedback to the move
        ui.item.css('background-color', '#FFFF9C')
          .animate({ backgroundColor: '#FFFFFF'}, 1500);
        ui.item.removeClass('grabbing');
      }
  };


 /*User clicks on Get Sections and the sections for that course
  gets added to the course scope*/
  $scope.getSections = function (courseId) {
    Sections.getSectionsForCourseId(courseId).then(function (data) {
      if (data) {
        //find the course object
        var coursePos = $scope.courses.indexOf(_.findWhere($scope.courses, {id: courseId}));
        //append a section object to the course scope
        $scope.courses[coursePos].sections = data.data;
        //sectionsShown = true hides the Get Sections link
        $scope.courses[coursePos].sectionsShown = true;
      } else {
        //deal with this
      }
    });
  };

  $scope.addUserModal = function(courseId){
    var course = $scope.courses.indexOf(_.findWhere($scope.courses, {id: courseId}));
    SectionSet.setSectionSet($scope.courses[course]);
  };
}]);



/* FRIEND PANEL CONTROLLER */
canvasSupportApp.controller('addUserController', ['Friend', '$scope', '$rootScope', 'SectionSet', function (Friend, $scope, $rootScope, SectionSet) {
  
  $scope.$on('courseSetChanged', function(event, sectionSet) {
      $scope.course = sectionSet[0];
  });
  $scope.checkAll = function(){
    $scope.oneChecked = false;
    for(var e in $scope.course.sections) {
      if ($scope.course.sections[e].isChecked) {
        $scope.oneChecked = true;
      }
    }
  };

  $scope.lookUpCanvasFriendClick = function () {
    $scope.friend = {};
    $scope.loading = true;
    var friendId = $.trim($('#friendEmailAddress').val());
    
    if(friendId.indexOf('@') !==-1 && friendId.indexOf('@umich.edu') ===-1){
      $scope.failedValidation = false;
      Friend.lookUpCanvasFriend(friendId).then(function (data) {
        if (data.data.length ===1 && data.data[0].name) {
          // TODO: check Friend account correlate
          // and if there is one, call this done, if not, create it
          $scope.friend = data.data[0];
          $scope.userAvailable = true;
        } else {
          // not an existing user - present interface to add
          // TODO: need to see if there is a Friend account correlate
          $scope.newUser = true;
        }
        $scope.loading = false;
      });
    }
    else {
      $scope.loading = false;
      $scope.failedValidation = true;
    }

  };
  $scope.createFriendClick = function () {

    var friendEmailAddress = $.trim($('#friendEmailAddress2').val());
    var friendNameFirst = $('#friendNameFirst').val();
    var friendNameLast = $('#friendNameLast').val();

    if(friendEmailAddress.indexOf('@') !==-1 && friendEmailAddress.indexOf('@umich.edu') ===-1){
      $scope.failedValidation = false;
      var requestorEmail = $rootScope.user.uniqname + '@umich.edu';
      $scope.done = false;
      $scope.loading2 = true;
      $scope.addSuccess = false;

      Friend.doFriendAccount(friendEmailAddress, requestorEmail).then(function (data) {
        //TODO: at some point the servlet will return message values
        //of 'created, exists, error, invalid' with a detailedMessage with the details
        //and we will need to change the string detecting below
        if (data.data.message === 'created' || data.data.message === 'exists') {
          $scope.friend_account = data.data;
          $scope.newUserFound=true;
          $scope.friendDone=true;
          
          Friend.createCanvasFriend(friendEmailAddress,friendNameFirst, friendNameLast).then(function (data) {
            if (data.data.name) {
              // here we add the person to the scope and then use another factory to add them to the sites
              $scope.newUser=false;
              $scope.newUserFound=true;
              $scope.userAvaliable = true;
              $scope.friend = data.data;//xxx
              $scope.canvasDone=true;
              //$scope.friend.sis_user_id = friendEmailAddress;
            } else {
              // TODO: report error
            }
            $scope.loading2 = false;
          });
          
          //$scope.friend = friendEmailAddress;
          $scope.userAvailable = true;
          $scope.done = true;
        } else {
          // TODO: report error
        }
      });
    }
    else {
      $scope.loading = false;
      $scope.failedValidation = true;
    }


  };

  $scope.addUserToSectionsClick = function () {
    for(var e in $scope.course.sections) {
      if ($scope.course.sections[e].isChecked) {
        var sectionId = $scope.course.sections[e].id;
        var sectionName = $scope.course.sections[e].name;
        var thisSectionRole = $('li[data-sectionid="'+sectionId+'"]').find('select').val();
        var url = '/sectionsUtilityTool/manager/api/v1/sections/' + sectionId + '/enrollments?enrollment[user_id]=' + $scope.friend.id + '&enrollment[type]=' + thisSectionRole;

        Friend.addFriendToSection(url).then(function (data) {
          if (data.errors) {
            // TODO: report error
          } else {
            if(data.data.course_id) {
              $scope.addSuccess = true;
              var successFullSections = $('#successFullSections').text();
              $('#successFullSections').text(successFullSections  + ' ' + sectionName);
            }
          }
        });
      }
    }
  };

}]);
