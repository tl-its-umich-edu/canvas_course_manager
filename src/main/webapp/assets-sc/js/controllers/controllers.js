'use strict';
/* global $, canvasSupportApp, getTermArray, _, getCurrentTerm, errorDisplay, validateUniqname, generateCurrentTimestamp */

/* TERMS CONTROLLER */
canvasSupportApp.controller('termsController', ['Courses', '$rootScope', '$scope', '$http', function (Courses, $rootScope, $scope, $http) {
  //void the currently selected term
  $scope.selectedTerm = null;
  //reset term scope
  $scope.terms = [];
  //REGEXINFO: canvas.api.terms.regex
  var termsUrl ='manager/api/v1/accounts/1/terms?per_page=4000&_=' + generateCurrentTimestamp();
  $http.get(termsUrl).success(function (data) {
    if(data.enrollment_terms){
      $scope.terms = data.enrollment_terms;
      $scope.$parent.currentTerm =  getCurrentTerm(data.enrollment_terms);
      $('.canvasTermIdforjQuery').text($scope.$parent.currentTerm.currentTermCanvasId);
      $('.canvasTermNameforjQuery').text($scope.$parent.currentTerm.currentTermName);
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
    $('.canvasTermIdforjQuery').text(termCanvasId);
    $('.canvasTermNameforjQuery').text(termName);
  };

}]);


//COURSES CONTROLLER
canvasSupportApp.controller('coursesController', ['Courses', 'Sections', '$rootScope', '$scope', 'SectionSet', function (Courses, Sections, $rootScope, $scope, SectionSet) {

 $scope.getCoursesForUniqname = function () {
    var uniqname = $.trim($('#uniqname').val().toLowerCase());
    $scope.uniqname = uniqname;
    //REGEXINFO: canvas.api.getcourse.by.uniqname.regex
    var url='/canvasCourseManager/manager/api/v1/courses?as_user_id=sis_login_id:' +uniqname+ '&per_page=100&published=true&with_enrollments=true&enrollment_type=teacher';
    $scope.loadingLookUpCourses = true;
    if (validateUniqname(uniqname)) {
      Courses.getCourses(url).then(function (result) {
        if (result.data[0].errors) {
          // the call to CAPI has returned a json with an error node
          if(uniqname) {
            // if the uniqname field had a value, report the problem (bad uniqname)
            $scope.errorMessage = result.data[0].errors + ' ' + '"' + uniqname + '"';
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
          $scope.loadingLookUpCourses = false;
        }
        else {
          if(result.errors){
            // catch all error
            $scope.success = false;
            $scope.error = true;
            $scope.instructions = false;
            $scope.loadingLookUpCourses = false;
          }
          else {
            // all is well - add the courses to the scope, extract the terms represented in course data
            // change scope flags and get the root server from the courses feed (!)
            var resultTeacher = result.data;
            //console.log('total courses where teacher role: ' + resultTeacher.length)
            if(result.data[0]){
              $rootScope.server = result.data[0].calendar.ics.split('/feed')[0];
            }
            //REGEXINFO: canvas.api.getcourse.by.uniqname.no.sections.regex
            var url='/canvasCourseManager/manager/api/v1/courses?as_user_id=sis_login_id:' +uniqname+ '&per_page=100&published=true&with_enrollments=true&enrollment_type=ta';
            Courses.getCourses(url).then(function (result) {
              //underscore _.uniq did not unique the concat of the two lists
              //so examine each of the TA role courses to see if it is already
              //in the Teacher role list
              _.each(result.data, function(tacourse){
                if( !_.findWhere(resultTeacher, {id: tacourse.id}) ) {
                  resultTeacher.push(tacourse);
                }
              });
              //console.log('total courses where teacher & ta role: ' + resultTeacher.length)
              $scope.courses = _.uniq(resultTeacher);
              //console.log('total courses where teacher & ta role, dupes removed: ' + $scope.courses.length)
              $scope.termArray = getTermArray(resultTeacher);
              $scope.error = false;
              $scope.success = true;
              $scope.instructions = true;
              $scope.errorLookup = false;
              if(result.data[0]){
                $rootScope.server = result.data[0].calendar.ics.split('/feed')[0];
              }
              $scope.loadingLookUpCourses = false;
              $rootScope.user.uniqname = uniqname;
            });
          }
        }
      });
    }
    else {
      $scope.loadingLookUpCourses = false;
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
    $scope.addSuccess = false;
    $scope.friend = {};
    $scope.loadingLookupFriend = true;
    var friendId = $.trim($('#friendEmailAddress').val());

    if(validateEmailAddress(friendId)){
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
        $scope.loadingLookupFriend = false;
      });
    }
    else {
      $scope.loadingLookupFriend = false;
      $scope.failedValidation = true;
    }

  };
  $scope.createFriendClick = function () {

    var friendEmailAddress = $.trim($('#friendEmailAddress2').val());
    var friendNameFirst = $('#friendNameFirst').val();
    var friendNameLast = $('#friendNameLast').val();

    if(validateEmailAddress(friendEmailAddress)){
      $scope.failedValidation = false;
      var requestorEmail = $rootScope.user.uniqname + '@umich.edu';
      $scope.done = false;
      $scope.loadingCreateUser = true;
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
            $scope.loadingCreateUser = false;
          });

          //$scope.friend = friendEmailAddress;
          $scope.userAvailable = true;
          $scope.done = true;
        } else {
          $scope.loadingCreateUser = false;
          $scope.friend_account = data.data;
          $scope.newUserFail=true;
          // TODO: report error
        }
      });
    }
    else {
      $scope.loadingCreateUser = false;
      $scope.failedValidation = true;
    }


  };

  $scope.addUserToSectionsClick = function () {
    var checkedSections = $('.addUserInner input:checked').length;
    var sectNumber = 0;
    $('#successFullSections').empty();
    for(var e in $scope.course.sections) {
      if ($scope.course.sections[e].isChecked) {
        sectNumber = sectNumber + 1;
        var sectionId = $scope.course.sections[e].id;
        var sectionName = $scope.course.sections[e].name;
        var thisSectionRole = $('li[data-sectionid="'+sectionId+'"]').find('select').val();
        //REGEXINFO: canvas.api.add.user.regex
        var url = '/canvasCourseManager/manager/api/v1/sections/' + sectionId + '/enrollments?enrollment[user_id]=' + $scope.friend.id + '&enrollment[enrollment_state]=active&enrollment[type]=' + thisSectionRole;

        Friend.addFriendToSection(url).then(function (data) {
          if (data.errors) {
            // TODO: report error
          } else {
            if(data.data.course_id) {
              $scope.addSuccess = true;
              if (checkedSections === sectNumber){
                $scope.newUser = false;
                $scope.newUserFound = false;
                $scope.done = false;
                $scope.friendEmailAddress ='';
                $scope.friend = {};
              }
            }
          }
        });

        $('#successFullSections').append(' <span class="label label-success">' + sectionName + '</span>');
      }
    }
  };

}]);
