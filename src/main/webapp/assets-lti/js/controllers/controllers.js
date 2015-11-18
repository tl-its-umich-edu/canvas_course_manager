'use strict';
/* global $, canvasSupportApp, getTermArray, _, getCurrentTerm, errorDisplay, generateCurrentTimestamp, angular, validateEmailAddress */

/* SINGLE COURSE CONTROLLER */
canvasSupportApp.controller('courseController', ['Course', 'Courses', 'Sections', 'Friend', 'SectionSet', 'Terms', 'focus', '$scope', '$rootScope', '$filter', function (Course, Courses, Sections, Friend, SectionSet, Terms, focus, $scope, $rootScope, $filter) {
  
  $scope.contextCourseId = $rootScope.ltiLaunch.custom_canvas_course_id;

  var courseUrl ='manager/api/v1/courses/' + $rootScope.ltiLaunch.custom_canvas_course_id + '?include[]=sections&_=' + generateCurrentTimestamp();
  Course.getCourse(courseUrl).then(function (result) {
    $scope.loadingSections = true;
    $scope.course = result.data;
    $rootScope.termId = $scope.course.enrollment_term_id;
    Sections.getSectionsForCourseId($scope.course.id).then(function (result) {
      $scope.loadingSections = false;
      $scope.course.sections =_.sortBy(result.data, 'name');
    });    
  });

  var termsUrl ='manager/api/v1/accounts/1/terms?per_page=4000&_=' + generateCurrentTimestamp();
  Terms.getTerms(termsUrl).then(function (result) {
    // this seems so unnecessary, getting all terms so that we can extract the sis_term_id of the Canvas term id
    $scope.currentTermSISID = _.where(result.data.enrollment_terms, {id:  $rootScope.termId}).sis_term_id
  });  
  
  /*
  KYLE: uncommenting this will just work - once we have a fix for the cosign thing
  adds to the scope a list of sections (by sis_section_id) that the current user can perform actions on

  var mPathwaysCoursesUrl = 'manager/mpathways/Instructors?instructor=' + $rootScope.ltiLaunch.custom_canvas_user_login_id +'&termid=2060';

  //var mPathwaysCoursesUrl = 'assets-lti/data/mpathwaysdata.json';
  Course.getMPathwaysCourses(mPathwaysCoursesUrl, $scope.currentTermSISID).then(function (result) {
    $scope.mpath_courses = result;
  });
  */

  $scope.getCoursesForTerm = function() {
    $scope.loadingOtherCourses = true;
    var coursesUrl='/canvasCourseManager/manager/api/v1/courses?as_user_id=sis_login_id:' + $rootScope.ltiLaunch.custom_canvas_user_login_id + '&per_page=200&published=true&with_enrollments=true&enrollment_type=teacher&_='+ generateCurrentTimestamp();
    Courses.getCourses(coursesUrl).then(function (result) {
      $scope.loadingOtherCourses = false;
      $scope.course.addingSections = true;
      // this is not optimal - ideally we should be requesting just this terms' courses, not all of them and then 
      // filtering them
      $scope.courses = _.where(result.data, {enrollment_term_id:  $rootScope.termId});
      $scope.$evalAsync(function() { 
        focus('otherCourses');
      })
    });    
  };

  $scope.getSections = function (courseId) {
    //find the course object
    var coursePos = $scope.courses.indexOf(_.findWhere($scope.courses, {id: courseId}));
    $scope.courses[coursePos].loadingOtherSections = true;
    Sections.getSectionsForCourseId(courseId).then(function (data) {
      if (data) {
        //append a section object to the course scope
        $scope.courses[coursePos].sections = _.sortBy(filterOutSections(data.data,$scope.mpath_courses), 'name');
        $scope.$evalAsync(function() { 
          focus('sections' + courseId);
        })


        $scope.courses[coursePos].loadingOtherSections = false;
        //sectionsShown = true hides the Get Sections link
        $scope.courses[coursePos].sectionsShown = true;
      } else {
        //deal with this
      }
    });
  };

  $scope.returnToCourse = function (section, thisindex) {
    var sourceCoursePos = $scope.courses.indexOf(_.findWhere($scope.courses, {id: section.course_id}));
    // move an added section back to the course it came from
    $scope.courses[sourceCoursePos].sections.push(section);
    // remove it from the target course section list
    $scope.course.sections.splice(thisindex, 1);
    // set dirty to true if any added sections remain
    $scope.course.dirty=false;
    _.each($scope.course.sections, function(section){
      if(section.course_id !== $scope.course.id){
        $scope.course.dirty=true;
      }
    });
  };

  $scope.appendToCourse = function (section, thisindex) {
    //find hte source course object
    var sourceCoursePos = $scope.courses.indexOf(_.findWhere($scope.courses, {id: section.course_id}));
    // add new section to the target course
    $scope.course.sections.push(section);
    // set dirty to true
    $scope.course.dirty=true;
    // remove section from source course
    $scope.courses[sourceCoursePos].sections.splice(thisindex, 1);
  };

  $scope.cancelAddSections = function () {
    // remove all added sections
    $scope.course.sections = $filter('filter')($scope.course.sections, {course_id: $scope.course.id});
    // set dirty and addingSections to false (controls display and disabled of buttons)
    $scope.course.dirty=false;
    $scope.course.addingSections = false;
    // prune the source courses from the model
    $scope.courses = [];
  };
  
  $scope.addUserModal = function(){
    // use a service to pass context course model to the friends controller
    SectionSet.setSectionSet($scope.course);
  };

}]);

/* FRIEND PANEL CONTROLLER */
canvasSupportApp.controller('addUserController', ['Friend', '$scope', '$rootScope', 'SectionSet', 'focus', function (Friend, $scope, $rootScope, SectionSet, focus) {
  // listen for changes triggered by the service to load course context
  $scope.$on('courseSetChanged', function(event, sectionSet) {
      $scope.coursemodal = sectionSet[0];
  });
  
  //change handler for section checkboxes - calculates if any checkbox is checked and updates
  // a variable used to enable the 'Add Friend' button
  $scope.sectionSelectedQuery = function () {
    if(_.where($scope.coursemodal.sections,{selected: true}).length > 0){

      $scope.coursemodal.sectionSelected = true;
    }
    else {
      $scope.coursemodal.sectionSelected = false;
    }
  };

  // handler for 'Add Friend' (the first one), if account exists in Canvas, add to sections, if not 
  // present a form to create

  $scope.lookUpCanvasFriendClick = function () {
    $scope.friend = {};
    $scope.coursemodal.loadingLookupFriend = true;
    var friendId = $.trim($scope.coursemodal.friendEmailAddress);
    if(validateEmailAddress(friendId)){
      $scope.failedValidation = false;
      Friend.lookUpCanvasFriend(friendId).then(function (data) {
        if (data.data.length ===1 && data.data[0].sis_user_id === friendId) {
          // user exists - set data to Canvas response
          // and call function to add to sections
          $scope.friend = data.data[0];
          $scope.userExists = true;
        } else {
          // not an existing user - present interface to add
          $scope.newUser = true;
          $scope.$evalAsync(function() { 
            focus('newUser');
          })

        }
        $scope.coursemodal.loadingLookupFriend = false;
      });
    }
    else {
      $scope.coursemodal.loadingLookupFriend = false;
      $scope.failedValidation = true;
      $scope.$evalAsync(function() { 
        focus('failedToValidateEmail');
      })
    }
  };

  // handler for 'Add Friend' (the second one) - calls Friends endpoint (that does the call to
  // the external Friend service) - if successful the account is also created in Canvas and 
  // the user gets added to the selected sections

  $scope.createFriendClick = function () {

    var friendEmailAddress = $.trim($scope.coursemodal.friendEmailAddress);
    var friendNameFirst = $.trim($scope.coursemodal.friendNameFirst);//$('#friendNameFirst').val();
    var friendNameLast = $.trim($scope.coursemodal.friendNameLast);//$('#friendNameLast').val();
    var notifyInstructor = 'false';

    if(validateEmailAddress(friendEmailAddress) && friendNameFirst !=='' && friendNameLast !==''){
      $scope.failedValidation = false;
      //will need to grab this from the LTI context and put it in the rootScope
      var requestorEmail = $rootScope.ltiLaunch.lis_person_contact_email_primary; // hardwired for now
      $scope.coursemodal.loadingCreateUser = true;

      Friend.doFriendAccount(friendEmailAddress, requestorEmail, notifyInstructor, $rootScope.ltiLaunch.lis_person_name_given, $rootScope.ltiLaunchlis_person_name_family).then(function (data) {
        if (data.data.message === 'created' || data.data.message === 'exists') {
          $scope.friend_account = data.data;
          $scope.newUserFound=true;
          $scope.friendDone=true;
          
          Friend.createCanvasFriend(friendEmailAddress,friendNameFirst, friendNameLast).then(function (data) {
            if (data.data.sis_user_id === friendEmailAddress) {
              // here we add the person to the scope and then use another function to add them to the sites
              $scope.newUser=false;
              $scope.newUserFound=true;
              $scope.friend = data.data;
              $scope.canvasDone=true;
              $scope.addUserToSectionsClick();
            } else {
              // TODO: report error
            }
            $scope.coursemodal.loadingCreateUser = false;
          });
          $scope.userAvailable = true;
          $scope.done = true;
        } else {
          $scope.coursemodal.loadingCreateUser = false;
          $scope.friend_account = data.data;
          $scope.newUserFail=true;
          // TODO: report error
        }
      });
    }
    else {
      $scope.failedValidation = true;
      $scope.$evalAsync(function() { 
        focus('failedToValidateEmailName');
      })

    }
  };

  // handler to reset the state in the workflow 
  // and a allow the user to add another

  $scope.addAnother = function() {
    $scope.friend = false;
    $scope.userExists = false;
    $scope.newUser = false;
    $scope.newUserFound = false;
    $scope.addSuccess = false;
    $scope.coursemodal.friendEmailAddress ='';
    $scope.coursemodal.friendNameFirst ='';
    $scope.coursemodal.friendNameLast ='';

    $scope.resetable = false;
    for(var e in $scope.coursemodal.sections) {
      $scope.coursemodal.sections[e].selected = false;
    }
    $scope.coursemodal.sectionSelected = false; 
  };

  // function used by the event handlers attached to the two 
  // 'Add Friend' buttons. It adds the user to the selected sections

  $scope.addUserToSectionsClick = function () {
    var checkedSections = $('.coursePanel input:checked').length;
    var sectNumber = 0;
    var successes = [];
    $('#successFullSections').empty();
    for(var e in $scope.coursemodal.sections) {
      if ($scope.coursemodal.sections[e].selected) {
        sectNumber = sectNumber + 1;
        var sectionId = $scope.coursemodal.sections[e].id;
        var sectionName = $scope.coursemodal.sections[e].name;
        var thisSectionRole = $('li#sect' +sectionId).find('select').val();
        
        var url = '/canvasCourseManager/manager/api/v1/sections/' + sectionId + '/enrollments?enrollment[user_id]=' + $scope.friend.id + '&enrollment[enrollment_state]=active&enrollment[type]=' + thisSectionRole;
        Friend.addFriendToSection(url).then(function (data) {
          if (data.errors) {
            // TODO: report error
          } else {
            if(data.data.course_id) {
              $scope.addSuccess = true;
              if (checkedSections === sectNumber){
                $scope.newUser = false;
                $scope.none = false;
                $scope.userAvailable  = false;
                $scope.coursemodal.resetable = true;
              }
            }
          }
        });
        $scope.addSuccess = true;
        successes.push(sectionName)
      }
      $scope.successes = successes

    }
      $scope.$evalAsync(function() { 
        focus('addMessageContainer');
      })


  };

}]);
