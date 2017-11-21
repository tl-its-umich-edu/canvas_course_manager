'use strict';
/* global $, canvasSupportApp, _, generateCurrentTimestamp, angular, validateEmailAddress, FileReader, document, setTimeout */

/* SINGLE COURSE CONTROLLER */
canvasSupportApp.controller('courseController', ['Course', 'Courses', 'Sections', 'Friend', 'SectionSet', 'Terms', 'focus', '$scope', '$rootScope', '$filter', '$location', '$log', function (Course, Courses, Sections, Friend, SectionSet, Terms, focus, $scope, $rootScope, $filter, $location, $log) {
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
        $rootScope.termId = $scope.course.enrollment_term_id;
        $scope.course = resultCourse.data;
        $scope.course.addingSections = false;
        $rootScope.courseAccount = $scope.course.account_id;
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


//top nav controller - it's sole reson for existence is turning off link to View X when you are in View X
canvasSupportApp.controller('navController', ['$scope', '$location', function ($scope, $location) {
  $scope.view='';
  $scope.changeView = function(view){
    $scope.view = view;
  };
}]);

/* SSA (Affiliate Functions) CONTROLLER */
canvasSupportApp.controller('saaController', ['Course', '$scope', '$rootScope', 'fileUpload', '$timeout', '$log', '$http', function(Course, $scope, $rootScope, fileUpload, $timeout, $log, $http) {
  // get the course scope - we will be using it for construction URLS and in the debug panel
  $scope.course = $rootScope.course;
  // get the sections already in the course we will use to validate adding users to sections
  //(section needs to exist) - store as a flat simple array of section sis_ids)
  $scope.availableSections = _.map(_.pluck($rootScope.sections, 'sis_section_id'), function(val){ return String(val); });

  // store the sections again as an array of objects - this is used by the grid (presenting the section name
  // but submitting the sis_id)
  $scope.availableSectionsGrid = [];
  _.each($rootScope.sections, function(section){
    if(section.sis_section_id){
      $scope.availableSectionsGrid.push({ 'name': section.name, 'id': section.sis_section_id });
    }
  });
  // get the existing groupsets, like above we store it as a flat array (to ensure CSV type is not using an existing one)
  var groupSetUrl = 'manager/api/v1/courses/' + $scope.course.id + '/group_categories';
  Course.getGroups(groupSetUrl).then(function (resultGroupsSets){
    $scope.availableGroupSets =
    _.map(
      resultGroupsSets.data,
      function(set) {
          return { 'name': set.name, 'id': set.id };
      }
    );
  });


  // functions.json contain the model (name, url, field list, validation rules for fields) for all interactions
  //CSV or form based
  $http.get('assets-lti/settings/functions.json').success(function(data) {
    $scope.functions = data;
    //add to the model for users and sections CSV the actual sections available in the course
    var functCSVSect = _.findWhere($scope.functions, {id: "users_in_sections"});
    var fieldCSVSect = _.findWhere(functCSVSect.fields, {name: "section_id"});
    fieldCSVSect.validation.choices = $scope.availableSections;
    //add to the model for users and sections GRID the actual sections available in the course
    var functCSVGrid = _.findWhere($scope.functions, {id: "users_to_sections_grid"});
    var fieldCSVGrid = _.findWhere(functCSVGrid.fields, {name: "section_id"});
    fieldCSVGrid.validation.choices = $scope.availableSections;
    fieldCSVGrid.validation.grid_choices = $scope.availableSectionsGrid;

    var groupUrl = 'manager/api/v1/courses/' + $scope.course.id + '/groups';

    //TODO: maybe remove this request - we are not using this info anywhere except in the debug panel
    Course.getGroups(groupUrl).then(function (resultGroups){
      //add to the model for groups the actual groups available in the course
      $scope.availableGroups = _.map(_.pluck(resultGroups.data, 'id'), function(val){ return String(val); });
    });
  });

  //listen for changes to the function chosen and reseting scope
  $scope.changeSelectedFunction = function() {
    $scope.resultPost = null;
    $scope.resetScope();
  };

  // reseting scope -  restoring model and view to defaults
  $scope.resetScope = function(){
    $('#gridTable input').val('');
    $scope.content={};
    $scope.errors=[];
    $scope.csv_fields=false;
    $scope.showErrors=false;
    $scope.throttleError=null;
    $scope.globalParseError=null;
    $scope.groupsParseError=null;
    $('#fileForm')[0].reset();
  };

  // on page load set content to false
  $scope.content = false;
  //grids will load 25 rows by default
  $scope.gridRowNumber = 25;
  // used by template in an ng-repeat using number above
  $scope.getNumber = function(num) {
    return new Array(num);
  };

  // watch for changes to input[type:file]
  // if new file read and parse the file
  $scope.$watch('csvfile', function(newFileObj) {
    $scope.content = false;
    if (newFileObj) {
      $scope.loading = true;
      var reader = new FileReader();
      reader.readAsText(newFileObj);
      // when done reading
      reader.onload = function(e) {
        // check that number of rows does not exceed throttle set in app.js
        if(reader.result.split('\n').length > $rootScope.csv_throttle && $scope.selectedFunction.id==='users_and_groups'){
          $timeout(function(){
            $scope.loading = false;
            $scope.throttleError = 'No more than ' + $rootScope.csv_throttle + ' rows in CSV allowed with this function';
          });
        }
        else {
          // use a function to parse, display and valdiate the data
          var CSVPreview = function() {
            $scope.headers = $scope.selectedFunction.fields;
            $scope.content = parseCSV(reader.result, $scope.headers, $scope.headers.length);
          };
          $timeout(CSVPreview, 100);
        }
      };
      $scope.filename = newFileObj.name;
    }
  });

  $scope.csvFileReset = function (){
     angular.element("input[type='file']").val(null);
  };

  // event handler for clicking on the Upload CSV button
  $scope.submitCSV = function() {
    var file = $scope.csvfile;
    // add new group set to the existing groupset array of objects
    // to update it so that the user does not add another file with the same groupset
    if($scope.selectedFunction.id ==='users_and_groups'){
      $scope.availableGroupSets.push({ 'name': $scope.newGroupSet, 'id': ''});
    }
    // use fileUpload service to handle the upload
    // params are file (the data), url (for this selectedFunction), and a callback to do some clean up
    fileUpload.uploadFileAndFieldsToUrl(file, $scope.selectedFunction.url, function(resultPost){
      $scope.resetScope();
      $scope.selectedFunction = null;
      // resultPost contains suceess and error payloads
      // UI does some contortions to display these correctly
      $scope.resultPost = resultPost;
    });
  };

  //event handler for submitting a grid form
  // read values, construct a blob and post it as a file
  $scope.submitGrid = function() {
      var result = document.getElementsByClassName("formRow");
      var wrappedResult = angular.element(result);
      var csv = _.pluck($scope.selectedFunction.fields,'name') + '\n';
      _.each (wrappedResult, function(thisRow, index){
        var thisRowArr = [];
         _.each($(thisRow).find('.select'), function(thisField, index){
           if(thisField.value !==''){
             thisRowArr.push(thisField.value);
           }
        });
        if (thisRowArr.length === $scope.selectedFunction.fields.length){
          csv = csv + thisRowArr.join(',') + '\n';
        }
      });

      $scope.groupGridErrors=false;
      if($scope.selectedFunction.id ==='users_and_groups_grid'){
        // special validations for this function, essentially
        // assuring that there is just one groupset and that users are not duplicated
        // and that the new groupset does not exist in the course already
        $scope.groupsParseError='';
        var arr = _.rest(csv.split('\n'));
        var groupsets=[];
        var users=[];
        _.each(arr, function(row){
          var rowArr = row.split(',');
          groupsets.push(rowArr[0]);
          users.push(rowArr[2]);
        });
        if(_.uniq(_.compact(groupsets)).length > 1){
          $scope.groupGridErrors=true;
          $scope.groupsParseError = $scope.groupsParseError + ' You have more than one groupset specified in the file. ';
        }
        if(_.compact(users).length !== _.uniq(_.compact(users)).length){
          $scope.groupGridErrors=true;
          $scope.groupsParseError = $scope.groupsParseError + ' You have duplicate users in the list.';
        }
        if(_.findWhere($scope.availableGroupSets, {name: _.uniq(_.compact(groupsets))[0] } ) !== undefined){
          $scope.groupGridErrors=true;
          $scope.groupsParseError = $scope.groupsParseError + ' There already is a groupset ' + _.uniq(_.compact(groupsets))[0] + ' with that name in the course. ';
        }
      }
      var file = csv;
      if($scope.groupGridErrors === false){
        // use same service and arg structure as CSV submission
        fileUpload.uploadFileAndFieldsToUrl(file, $scope.selectedFunction.url, function(resultPost){
          $scope.resetScope();
          $scope.selectedFunction = null;
          $scope.resultPost = resultPost;
        });
      }
  };

  //parse attached CSV and validate it against functions model
  var parseCSV = function(CSVdata, headers, colCount) {
    // initialize certain scope vars
    $scope.content={};
    $scope.errors=[];
    $scope.globalParseError=null;
    //remove certain line break chars
    CSVdata = CSVdata.replace(/\r/g, '');
    //create an array of lines
    var lines = CSVdata.split("\n");
    //only one line - line endings may be funky
    if(lines.length === 1){
      $scope.loading = false;
      $scope.globalParseError ='Something is wrong with your file. Is it standard CSV format?';
      return null;
    }
    //grab first row
    var linesHeaders = lines[0].split(',');

    // check that first row has same number of elements required by the selectedFunction json
    if((_.difference(linesHeaders, $scope.selectedFunction.field_array).length)){
       $scope.globalParseError ='Something is wrong with your file.  Bad or missing headers? Should be: \"' + $scope.selectedFunction.field_array.join(', ') + '\"';
       $scope.loading = false;
       return null;
    }

    //remove leading and trailing spaces
    for (var i = 0; i < linesHeaders.length; i++) {
      linesHeaders[i] = linesHeaders[i].trim();
    }

    // get all lines except the header line
    var linesValues = _.rest(lines,1);
    // more line ending paranoia
    if(linesValues.length === 0){
      $scope.loading = false;
      $scope.globalParseError ='Something is wrong with your file. Line endings?';
      return null;
    }
    //initialize final object
    var result = {};
    result.data =[];
    $scope.errors = [];
    $scope.log =[];

    // sort the headers in the functions.json to match the order in the
    // first line of the CSV - this makes order in the CSV inmmaterial
    var sortedHeaders =[];
    _.each (linesHeaders, function(lineHeader, index){
      var header = _.findWhere(headers, {name:lineHeader});
      sortedHeaders.push(header);
    });
    // create array of this line
    for (var i = 0; i < linesValues.length; i++) {
      var lineArray = linesValues[i].split(',');
      // initialize an line object
      var lineObj = {};
      lineObj.data = [];
      var number_pattern = /^\d+$/;
      // for each of the headers
      _.each(sortedHeaders, function(header, index) {
        //initialize object for this value
        var obj = {};
        obj.invalid = false;
        obj.message='';
        // last paranoid check
        if(header===undefined){
          $scope.loading =false;
          $scope.globalParseError = "Something is wrong with your file. Is it standard CSV format?";
          return null;
        }
        // grab the .validation value fo this funciton from functions.json
        var validation = header.validation;

        if (lineArray[index]) {
          //remove leading and trailing spaces
          var thisVal = lineArray[index].trim();
          // does it have spaces and spaces are forbidden?
          if (thisVal.split(' ').length !== 1 && !validation.spaces) {
            $scope.log.push(i+1 + ' - "' + thisVal + '" has spaces');
            obj.message = obj.message + thisVal + ' has spaces';
            obj.invalid = true;
            lineObj.invalid=true;
          }
          // more chars than allowed?
          if (thisVal.length > validation.max) {
            $scope.log.push(i+1 + ' - "' + thisVal + '" has too many chars');
            obj.message = obj.message + thisVal + ' has too many chars';
            obj.invalid = true;
            lineObj.invalid=true;
          }
          // less chars than allowed?
          if (thisVal.length < validation.min) {
            $scope.log.push(i+1 + ' - "' + thisVal + '" has too few chars');
            obj.message = obj.message + thisVal + ' has too few chars';
            obj.invalid = true;
            lineObj.invalid=true;
          }
          // if validation has a pattern  - test this
          if(validation.pattern){
            var pat = new RegExp(validation.pattern);
            if (!pat.test(thisVal)) {
              $scope.log.push(i+1 + ' - "' + thisVal + ' ' + validation.val_message);
              obj.message = obj.message + thisVal + ' ' + validation.val_message;
              obj.invalid = true;
              lineObj.invalid=true;
            }
          }
          // is it supposed to be a number?
          if (!number_pattern.test(thisVal) && validation.chars === 'num') {
            $scope.log.push(i+1 + ' - "' + thisVal + '" is not a number');
            obj.message = obj.message + thisVal + ' is not a number';
            obj.invalid = true;
            lineObj.invalid=true;
          }
          // are there specific choices that this value needs to be?
          // think adding users and sections - sections need to be there already - we added the sections above
          if (validation.choices) {
            if (_.indexOf(validation.choices, thisVal) === -1) {
              $scope.log.push(i+1 + ' - "' + thisVal + '" is not one of the choices in [' + validation.choices + ']');
              obj.message = obj.message + thisVal + '  is not one of the choices in [' + validation.choices + ']';
              obj.invalid = true;
              lineObj.invalid=true;
            }
          }
        }
        //push the result to the lineObj.data array
        lineObj.data.push({'value':lineArray[index],'error':obj.invalid,'message':obj.message});
      });
      // check for missing values
      if (lineArray.length !== colCount && lineArray !== ['']) {
        lineObj.invalid = true;
      }



      if (lineArray.length === 1 && lineArray[0] === '') {

      } else {
        // push the line to the result
        result.data.push(lineObj);
      }
    }
    if($scope.selectedFunction.id==='users_and_groups'){
      // special validation for users and groups
      // 1. header order
      // 2. no more than 1 groupset name
      // 3. existing groupsets
      // 4. duplicate user entries
      var groupsetMap = [];
      var userMap = [];

      $scope.groupsParseError ='';

      if(!angular.equals(linesHeaders, $scope.selectedFunction.field_array)){
        $scope.groupsParseError = 'The headers in the file do not match the required header set or order.';
      }

      _.each(result.data, function(thisData){
        groupsetMap.push(thisData.data[0].value);
        userMap.push(thisData.data[2].value);
      });
      // check that there is only one groupset and that it is not contained in groupsets existing
      if (_.uniq(groupsetMap).length > 1){
        $scope.groupsParseError = $scope.groupsParseError + ' You have more than one groupset specified in the file. ';
      }
      else {
        $scope.newGroupSet = groupsetMap[0];
      }
      if(_.findWhere($scope.availableGroupSets, {name: _.uniq(groupsetMap)[0] } ) !== undefined){
        $scope.groupsParseError = $scope.groupsParseError + ' There already is a groupset with that name in the course. ';
      }
      // check that there is no dupe users
      if(_.uniq(userMap).length !== userMap.length){
        $scope.groupsParseError = $scope.groupsParseError + ' You have duplicate users in the list.';
      }
    }

    //stop the spinner
    $scope.loading = false;
    result.headers = linesHeaders;
    $('html, body').animate({
      scrollTop : 300
    }, 1000);
    // pass the result to the view via a model change
    return result;

  };
}]);

canvasSupportApp.controller('gradesController', ['$scope', '$location', '$rootScope', '$log', '$timeout', function ($scope, $location, $rootScope, $log, $timeout) {
  // empty controller for grades view
}]);
