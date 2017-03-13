'use strict';
/* global $, canvasSupportApp, _, generateCurrentTimestamp, angular, validateEmailAddress */

/* SINGLE COURSE CONTROLLER */
canvasSupportApp.controller('courseController', ['Course', 'Courses', 'Sections', 'Friend', 'SectionSet', 'Terms', 'focus', 'SAA', '$scope', '$rootScope', '$filter', '$location', '$log', function (Course, Courses, Sections, Friend, SectionSet, Terms, focus, SAA, $scope, $rootScope, $filter, $location, $log) {
    $scope.userIsFriend=false;
    $scope.contextCourseId = $rootScope.ltiLaunch.custom_canvas_course_id;
    $scope.currentUserCanvasId = $rootScope.ltiLaunch.custom_canvas_user_id;
    $scope.currentUserId = $rootScope.ltiLaunch.custom_canvas_user_login_id;
    $scope.canAddTeachers = JSON.parse($rootScope.ltiLaunch.role_can_add_teacher);
    if($scope.currentUserId.indexOf('+') > -1){
      $scope.currentUserId = $scope.currentUserId.replace('+','@');
      $scope.userIsFriend=true;
    }
    //get the current user id from launch params
    $scope.canvas_user_id = $scope.currentUserCanvasId;
    //REGEXINFO: canvas.api.get.single.course.regex
    var courseUrl ='manager/api/v1/courses/course_id?include[]=sections&with_enrollments=true&enrollment_type=teacher&_=' + generateCurrentTimestamp();
    Course.getCourse(courseUrl).then(function (resultCourse) {
      if(!resultCourse.data.errors) {
        $scope.loadingSections = true;
        $rootScope.course = resultCourse.data;
        $scope.course = resultCourse.data;
        $scope.course.addingSections = false;
        $rootScope.termId = $scope.course.enrollment_term_id;

        $scope.course = resultCourse.data;
        $rootScope.courseAccount = $scope.course.account_id;
        $rootScope.account ={};
        $rootScope.account.enabled = false;
        var accountUrl = 'manager/api/v1/accounts?as_user_id=sis_login_id:' + $rootScope.ltiLaunch.custom_canvas_user_login_id;
        SAA.getAccounts(accountUrl).then(function (resultAccount) {

          if (resultAccount.data.length ===1 && (resultAccount.data[0].id === $rootScope.courseAccount)){
            $rootScope.account.enabled = true;
            $rootScope.account.rootAccountId = resultAccount.data[0].id;
            $rootScope.account.rootAccountName = resultAccount.data[0].name;
          } else {
            var subAccountsUrl = 'manager/api/v1/accounts/' + resultAccount.data[0].id  + '/sub_accounts?recursive=true&per_page=200';
            SAA.getAccounts(subAccountsUrl).then(function (resultSubAccounts) {
              var goodAccount =_.findWhere(resultSubAccounts.data, {id: $rootScope.courseAccount});
              if(goodAccount) {
                $rootScope.account.enabled = true;
                $rootScope.account.rootAccountId = resultAccount.data[0].id;
                $rootScope.account.rootAccountName = resultAccount.data[0].name;
                $rootScope.account.subAccountId = goodAccount.id;
                $rootScope.account.subAccountName = goodAccount.name;
              }
            });
          }
        });

        Sections.getSectionsForCourseId('', true).then(function (resultSections) {
          $rootScope.sections = resultSections.data;
          $scope.loadingSections = false;
          if(!resultSections.data.errors) {
            $scope.course.sections =_.sortBy(resultSections.data, 'name');
            if($scope.course.sections[0].sis_course_id) {
              $scope.currentTermSISID = $scope.course.sections[0].sis_course_id.substring(0, 4);
            }
            else {
             $scope.currentTermSISID = $scope.course.sections[0].sis_course_id;
            }
            if($scope.currentTermSISID) {
            /* adds to the scope a list of sections (by sis_section_id) that the current user can perform actions on */
            var mPathwaysCoursesUrl = 'manager/mpathways/Instructors?user=self&termid=' + $scope.currentTermSISID;
              Course.getMPathwaysCourses(mPathwaysCoursesUrl, $scope.currentTermSISID).then(function (resultMPathData) {
                if(!resultMPathData.data) {
                  if(Array.isArray(resultMPathData)) {
                    $scope.mpath_courses = resultMPathData;
                  }
                } else {
                  $scope.mpath_courses =[];
                  $scope.mpath_courses_error =true;
                }
              });
            }
            else {
              $scope.mpath_courses =[];
            }
          }
        });
      }
    });
    //REGEXINFO: canvas.api.get.single.course.enrollment.regex
    var courseEnrollmentUrl ='manager/api/v1/courses/course_id/enrollments?user_id=' + $scope.canvas_user_id + '&_=' + generateCurrentTimestamp();

    Course.getCourse(courseEnrollmentUrl).then(function (resultCourseEnrollment) {
      $rootScope.courseRole = teacherPrivileges(resultCourseEnrollment.data, $scope.canAddTeachers);
    });

  $scope.getCoursesForTerm = function() {
    $scope.loadingOtherCourses = true;
    //$rootScope.courseRole
    //REGEXINFO: canvas.api.getcourse.by.uniqname.no.sections.mask.regex
    //TODO: Remove per page query
    var coursesUrl='/canvasCourseManager/manager/api/v1/courses?user=self&per_page=50&published=true&with_enrollments=true';
    Courses.getCourses(coursesUrl).then(function (resultCourses) {
      $scope.loadingOtherCourses = false;
      $scope.course.addingSections = true;
      // this is not optimal - ideally we should be requesting just this terms' courses, not all of them and then
      // filtering them
      //filter term
      var filteredByTerm = _.where(resultCourses.data, {enrollment_term_id:  $rootScope.termId});
      // filter by enrollment
      // will need to get enrollments and cycle through them, if not teacher or ta, disable section move
      var filteredByRole = filterByRole(filteredByTerm);
      //remove possible dupes
      $scope.courses = _.uniq(filteredByRole);

      $scope.$evalAsync(function() {
        focus('otherCourses');
      })
    });
  };

  $scope.getSections = function (courseId) {
    //find the course object
    var coursePos = $scope.courses.indexOf(_.findWhere($scope.courses, {id: courseId}));
    $scope.courses[coursePos].loadingOtherSections = true;
    Sections.getSectionsForCourseId(courseId, false).then(function (resultSections) {
      if (resultSections) {
        //append a section object to the course scope
        $scope.courses[coursePos].sections = _.sortBy(filterOutSections(resultSections.data,$scope.mpath_courses), 'name');
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
    // hide the "Done" button
    $scope.course.addingSectionsDone = false;
  };

  $scope.xListSections = function(courseId){
    // get the added sections by rejecting all of the original sections
    // and keeping the remainder
    var addedSections = _.reject($scope.course.sections, {course_id : courseId});
    // for each added section call a factory that will do a post
    $scope.course.xLists =[];
    _.each(addedSections, function(section){
      var xListUrl = 'manager/api/v1/sections/' + section.id + '/crosslist/course_id';
      Course.xListSection(xListUrl).then(function (resultXList) {
        $scope.course.xLists.push(section.name);
        section.course_id = courseId;
        if (addedSections.length === $scope.course.xLists.length) {
          $scope.course.dirty = false;
          $scope.course.addingSections = false;
          $scope.course.addingSectionsDone = true;
        }
      });
    });
  };

  $scope.addUserModal = function(){
    _.each($scope.course.sections, function(section){
      section.courseRole=$rootScope.courseRole;
    });

    // use a service to pass context course model to the friends controller
    SectionSet.setSectionSet($scope.course);
  };
  $scope.showInfo = function(){
    $scope.showPop = !$scope.showPop;
  }
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
      Friend.lookUpCanvasFriend(friendId).then(function (resultLookUpCanvasFriend) {
        if(resultLookUpCanvasFriend.status ===200) {
          if (resultLookUpCanvasFriend.data.length ===1 && resultLookUpCanvasFriend.data[0].sis_user_id === friendId) {
            // user exists - set data to Canvas response
            $scope.friend = resultLookUpCanvasFriend.data[0];
            $scope.userExists = true;
          } else {
            // not an existing user - present interface to add
            $scope.newUser = true;
            $scope.$evalAsync(function() {
              focus('newUser');
            })

          }
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
    var friendNameFirst = $.trim($scope.coursemodal.friendNameFirst);
    var friendNameLast = $.trim($scope.coursemodal.friendNameLast);

    var notifyInstructor = 'false';

    if(validateEmailAddress(friendEmailAddress) && friendNameFirst !=='' && friendNameLast !==''){
      $scope.failedValidation = false;
      var requestorEmail = $rootScope.ltiLaunch.lis_person_contact_email_primary;
      $scope.coursemodal.loadingCreateUser = true;

      Friend.doFriendAccount(friendEmailAddress, requestorEmail, notifyInstructor, $rootScope.ltiLaunch.lis_person_name_given, $rootScope.ltiLaunch.lis_person_name_family).then(function (resultDoFriendAccount) {
        //check for success of creating a friend account (or if it is already there)
        if (resultDoFriendAccount.data.message === 'created' || resultDoFriendAccount.data.message === 'exists') {
          $scope.friend_account = resultDoFriendAccount.data;
          $scope.newUserFound=true;
          $scope.friendDone=true;

          Friend.createCanvasFriend(friendEmailAddress,friendNameFirst, friendNameLast).then(function (resultCreateCanvasFriend) {
            // check for successufull creation of Canvas account
            if (resultCreateCanvasFriend.data.sis_user_id === friendEmailAddress) {
              // here we add the person to the scope and then use another function to add them to the sites
              $scope.newUser=false;
              $scope.newUserFound=true;
              $scope.friend = resultCreateCanvasFriend.data;
              $scope.canvasDone=true;
              $scope.addUserToSectionsClick();
            } else {
              // TODO: servlet errors are caught by factory
              // here we would deal with a 200 that nevertheless was an error, but have been unable to trigger this
            }
          });
          $scope.userAvailable = true;
          $scope.done = true;
        } else {
          // 500 errors are caught and reported by factory, here we
          // are dealing with incorrect data errors (ie. email address that slipped through validator)
          if(data.data.message !== 'request error') {
            $scope.friend_account = resultCreateCanvasFriend.data;
            $scope.newUserFail=true;
          }
        }
        $scope.coursemodal.loadingCreateUser = false;
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
    $scope.successes = false;
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
    var checkedSections = _.where($scope.coursemodal.sections, {selected: true}).length;
    var sectNumber = 0;
    var successes = [];
    var errors = [];
    $('#successFullSections').empty();
    for(var e in $scope.coursemodal.sections) {
      if ($scope.coursemodal.sections[e].selected) {
        sectNumber = sectNumber + 1;
        var sectionId = $scope.coursemodal.sections[e].id;
        var sectionName = $scope.coursemodal.sections[e].name;
        var thisSectionRole = $('li#sect' +sectionId).find('select').val();
        //REGEXINFO: canvas.api.add.user.regex
        var url = '/canvasCourseManager/manager/api/v1/sections/' + sectionId + '/enrollments?enrollment[user_id]=' + $scope.friend.id + '&enrollment[enrollment_state]=active&enrollment[type]=' + thisSectionRole;
        Friend.addFriendToSection(url, sectionName, sectNumber).then(function (resultAddFriendToSection) {
          if(resultAddFriendToSection.data[1].message){
            $scope.addErrorGeneric = resultAddFriendToSection.data[1].message;
          }
          else {
            if (resultAddFriendToSection.data.errors) {
              // failed to process this add
              errors.push(sectionName);
              $scope.addError = true;
            } else {
              if(resultAddFriendToSection.data[1].course_id) {
                // was able to process this add
                successes.push(resultAddFriendToSection.data[0].section_name);
                if (checkedSections === resultAddFriendToSection.data[0].section_number){
                  // the last request, clean up the scope
                  $scope.newUser = false;
                  $scope.none = false;
                  $scope.userAvailable  = false;
                  $scope.coursemodal.resetable = true;
                }
              }
              else {
                errors.push(sectionName);
              }
            }
          }
        });
      }
    }
    // if a single failure, toggle error message
    if($scope.addError || $scope.addErrorGeneric) {
      $scope.addSuccess = false;
    } else {
      $scope.addSuccess = true;
    }
    // make available to the template what sections succeeded, which not
    $scope.successes = successes.sort();
    $scope.errors = errors
    // pass the focus to the container of the success and failure message
    $scope.$evalAsync(function() {
      focus('addMessageContainer');
    })
  };
}]);


canvasSupportApp.controller('navController', ['$scope', '$location', function ($scope, $location) {
  $scope.changeView = function(view){
    $scope.view = view;
  };
}]);

/* SSA (Affiliate Functions) CONTROLLER */
canvasSupportApp.controller('saaController', ['Course','SectionSet', '$scope', '$rootScope', 'fileUpload', '$timeout', '$log', '$http', function(Course, SectionSet, $scope, $rootScope, fileUpload, $timeout, $log, $http) {
  $scope.course = $rootScope.course;
  $scope.availableSections = _.map(_.pluck($rootScope.sections, 'sis_section_id'), function(val){ return String(val); });

  $http.get('assets-lti/settings/functions.json').success(function(data) {
    $scope.functions = data;
    var functCSVSect = _.findWhere($scope.functions, {id: "users_in_sections"});
    var fieldCSVSect = _.findWhere(functCSVSect.fields, {name: "section_id"});
    fieldCSVSect.validation.choices = $scope.availableSections;
    var functCSVGrid = _.findWhere($scope.functions, {id: "users_to_sections_grid"});
    var fieldCSVGrid = _.findWhere(functCSVGrid.fields, {name: "section_id"});
    fieldCSVGrid.validation.choices = $scope.availableSections;

    var groupUrl = 'manager/api/v1/courses/' + $scope.course.id + '/groups';
    Course.getGroups(groupUrl).then(function (resultGroups){
      $scope.availableGroups = _.map(_.pluck(resultGroups.data, 'id'), function(val){ return String(val); });
      var functCSVGrp = _.findWhere($scope.functions, {id: "users_to_groups"});
      var fieldCSVGrp = _.findWhere(functCSVGrp.fields, {name: "group_id"});
      fieldCSVGrp.validation.choices = $scope.availableGroups;
      var functGrpGrid = _.findWhere($scope.functions, {id: "users_to_groups_grid"});
      var fieldGrpGrid = _.findWhere(functGrpGrid.fields, {name: "group_id"});
      fieldGrpGrid.validation.choices = $scope.availableGroups;
    });
  });

  $scope.changeSelectedFunction = function() {
    $rootScope.selectedFunction = value;
  };


  $scope.content = false;
  $scope.gridRowNumber = 25;
  $scope.getNumber = function(num) {
    return new Array(num);
  };

  $scope.$watch('csvfile', function(newFileObj) {
    if (newFileObj) {
      $scope.content = false;
      $scope.loading = true;
      var reader = new FileReader();
      reader.readAsText(newFileObj);
      reader.onload = function(e) {
        var CSVPreview = function() {
          $scope.headers = $scope.selectedFunction.fields;
          $scope.content = parseCSV(reader.result, $scope.headers, $scope.headers.length);
        };
        $timeout(CSVPreview, 100);
      };
      $scope.filename = newFileObj.name;
    }
  });

  $scope.changeSelectedFunction = function() {
    $scope.content = [];
  };


  $scope.submitCSV = function() {
    var file = $scope.csvfile;
    //var uploadUrl = "/api/v1/accounts/1/sis_imports?override_sis_stickiness=false&batch_mode=false&import_type=instructure_csv&extension=csv&attachment=" + $scope.filename;
    var uploadUrl = '/uploadCSV';
    // var fields = {
    //   "name": "filename",
    //   "user": "gsilver",
    //   "request": "users_to_sections",
    //   "account": 12,
    //   "data": $scope.filename
    // };
    fileUpload.uploadFileAndFieldsToUrl(file, uploadUrl);
  };

  $scope.submitGrid = function() {
    var uploadUrl = "/formUpload";
    var fields = {
      "name": "filename",
      "user": "gsilver",
      "request": "users_to_sections",
      "account": 12,
      "data": $scope.filename
    };

  };




  var parseCSV = function(CSVdata, headers, colCount) {
    var lines = CSVdata.split("\n");
    var result = [];
    $scope.errors = [];
    for (var i = 0; i < lines.length; i++) {
      var lineArray = lines[i].split(',');
      var obj = {};
      obj.data = [];
      var number_pattern = /^\d+$/;
      _.each(headers, function(header, index) {
        var validation = header.validation;
        if (lineArray[index]) {
          if (lineArray[index].split(' ').length !== 1 && !validation.spaces) {
            $log.warn(lineArray[index] + ' has spaces');
            obj.invalid = true;
          }
          if (lineArray[index].length > validation.max) {
            $log.warn(lineArray[index] + ' too many chars');
            obj.invalid = true;
          }
          if (lineArray[index].length < validation.min) {
            $log.warn(lineArray[index] + ' too few chars');
            obj.invalid = true;
          }
          if (!number_pattern.test(lineArray[index]) && validation.chars === 'num') {
            $log.warn(lineArray[index] + ' not a number');
            obj.invalid = true;
          }
          if (validation.choices) {
            if (_.indexOf(validation.choices, lineArray[index]) === -1) {
              $log.warn(lineArray[index] + ' is not one of the choices in ' + validation.choices);
              obj.invalid = true;
            }
          }
        }
        obj.data.push(lineArray[index]);
      });
      if (lineArray.length !== colCount && lineArray !== ['']) {
        obj.invalid = true;
      }

      if (lineArray.length === 1 && lineArray[0] === '') {

      } else {
        result.push(obj);
      }
    }
    if (_.where(result, {invalid: true}).length) {
      $scope.errors = _.where(result, {invalid: true});
    }
    $scope.loading = false;
    return result;

  };

}]);
