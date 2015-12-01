'use strict';
/* jshint  strict: true*/
/* global $, moment, _*/

/**
 * set up global ajax options
 */
$.ajaxSetup({
  type: 'GET',
  dataType: 'json',
  cache: false
});

// generic error report
var errorDisplay = function (url, status, errorMessage) {
  switch(status) {
    case 403:
      window.location = '/canvasCourseManager/error.html';
      break;
    default:
      $('#debugPanelBody').html('<h3>' + status + '</h3><p><code>' + url + '</code></p><p>' + errorMessage + '</p>');
      $('#debugPanel').fadeIn();
  }
};

// provide a timestamp to add to angular GETs to invalidate cache
// Matches jQuery parameter when cache: false - makes regexp whitelist
// easier to manage
var generateCurrentTimestamp = function(){
  return new Date().getTime();
};

var prepareMPathData = function(MPathData, sis_term_id) {
  var mPathArray = [];
  // MPath via ESB will return an object if a single course, an array if more than one
  /// so turn everything into an array
  var arrayMPath = [].concat(MPathData.Result.getInstrClassListResponse.InstructedClass);
  
  $.each(arrayMPath, function() {
    if(this.InstructorRole === 'Primary Instructor' || this.InstructorRole === 'Seconday Instructor'  || this.InstructorRole === 'Faculty grader'  || this.InstructorRole === 'Graduate Student Instructor'){
      mPathArray.push(sis_term_id + this.ClassNumber);
    }
  });
  return mPathArray;
};

var filterOutSections = function(sectionData, mPathArray){
  $.each(sectionData, function() {
    if(_.contains(mPathArray, this.sis_section_id)){
      this.enabled = true;
    }
  });
  return sectionData;
};

// specific success reporting, used in the jQuery requests
var reportSuccess = function(position, msg){
  $('#successContainer').css('top', position);
  $('#successContainer').find('.msg').html(msg);
  $('#successContainer').fadeIn().delay(3000).fadeOut();
};
// specific error reporting, used in the jQuery requests
var reportError = function(position, msg){
  $('#errorContainer').css('top', position);
  $('#errorContainer').find('.msg').html(msg);
  $('#errorContainer').fadeIn();
};

/**
 * Pop a window to display help matters, name allows to pass the 
 * focus to it if already open and in the background
 * 
 * @param {Object} url 
 * @param {Object} name - window name
 */
var utilPopWindow = function(url, name){
    var notAModal = window.open(url, name, 'height=800,width=600, toolbar=yes, menubar=yes, scrollbars=yes, resizable=yes');
    if (window.focus) {
        notAModal.focus();
    }
    return false;
};

/*used by modal (other instructor field) */
var validateUniqname = function (value) {
  var value = $.trim(value);
  var letterOnly = /^[a-z]+$/i;  
  if(value.match(letterOnly) && value !=='') {  
    return true;
  } else {
    return false;
  }
};


/*used by adding friend
  TODO: can we use some standard industrial strength validator?
 */
var validateEmailAddress = function (value) {
  var value = $.trim(value);
  if(value.indexOf('@') !==-1 &&
    value.indexOf('@umich.edu') ===-1  && 
    value.split('@').length === 2 && 
    value.split('@')[0] !== '' && 
    value.split('@')[1] !== '' &&
    value.split('@')[1].split('.').length > 1){
    return true;
  } else {
    return false;
  }
};

var xListPostStatus;

// function used in xlist multiple post - returns a referred that the calling 
// function can use to determine success/errors - it also adds labels for each
// case in the modal
var doXListPosts = function(posts){
  var index, len;
  var xListPosts = [];
  xListPostStatus = {successes: [], failures: []};
  for (index = 0, len = posts.length; index < len; ++index) {
    var xListUrl ='manager' + posts[index];    
    xListPosts.push(
      $.post(xListUrl, function(data) {
        var section = data.id;
        xListPostStatus.successes.push(data);
        $('#xListSection' +  section).find('.xListStatus').html(' <span class=\"label label-success\">Success</span>');
      })
      .fail(function(data) {
        var section = data.id;
        xListPostStatus.failures.push(data);
        $('#xListSection' +  section).find('.xListStatus').html(' <span class=\"label label-failure\">Failure</span>');
      })
    );
  }
    return xListPosts;
};


/**
 *
 * event watchers
 */

//open help doc in new window
$('#helpLink').click(function(){
	utilPopWindow('help.html', 'help');
});

//handler for the Update Course button
$(document).on('click', '.setSections', function (e) {
  var server = $('#serverInfo').text();
  $('#postXListDone').hide();
  $('#postXList').show();
  $('#xListConfirm').hide();
  e.preventDefault();
  $('#debugPanel').empty();
  var thisCourse = $(this).attr('data-courseid');
  $('#postXList').attr('course-id', thisCourse);

  var thisCourseContainer = $(this).closest('li.course');
  var thisCourseTitle = thisCourseContainer.find('a.courseLink').text();

  var $sections = $(this).closest('li').find('ul').find('li');
  
  var posts = [];
  $('#xListInner').empty();
  $('#xListInner').append('<p><strong>' + thisCourseTitle + '</strong></p><ol id="listOfSectionsToCrossList" class="listOfSectionsToCrossList"></ol>');

  $('#xListConfirm').attr('href',server + '/courses/' + thisCourse + '/settings#tab-sections');
  $sections.each(function( ) {
    $('#listOfSectionsToCrossList').append( '<li data-sectionid=\"' + $(this).attr('data-sectionid') + '\" ' + 'id=\"xListSection' + $(this).attr('data-sectionid') + '\">' + 
      '<span class=\"xListStatus\"></span> ' + $(this).find('div.sectionName span').text() + '</li>');
  });
  return null;
});

//handle the  button in modal that triggers the crosslist posts
$(document).on('click', '#postXList', function () {
  var thisCourse = $('#postXList').attr('course-id');
  $('#postXList, #postXListCancel').hide();

  var thisCourseContainer = $('li.course[data-course-id="' + thisCourse + '"]');
  
  var $sectionsInModal = $('#listOfSectionsToCrossList').find('li');
  var posts = [];
  $sectionsInModal.each(function( ) {
    posts.push('/api/v1/sections/' + $(this).attr('data-sectionid') + '/crosslist/' + thisCourse);
  });

  var xListPosts = doXListPosts(posts);
  $.when.apply($, xListPosts).done(function() {
    if(xListPostStatus.successes.length === posts.length ){
      $('#xListConfirmMessage').text(posts.length + ' sections crosslisted. ');
    }
    else {
      $('#xListConfirmMessage').text(xListPostStatus.failures.length + ' error(s). ');
    }
    $('#postXListDone, #xListConfirm').show();
    $('.activeCourse').removeClass('activeCourse');
    $(thisCourseContainer).find('.setSections').fadeOut().delay(5000).hide();
  });
});  

$(document).on('click', '.sectionCheck', function (e) {
  e.preventDefault();
  var thisSection = $(this).closest('li.section');
  var thisSectionId = thisSection.attr('data-sectionid');
  $('#unCrossListInner').empty();
  $('#unCrossListInner').text('Uncrosslisting will re-associate this section with the previous course.'); 
  $('#unCrossList').show();
  $('#unCrossList').attr('data-section-id', thisSectionId);
  return null;
});  

//cleaning up scope of adding friend panel
$(document).on('hidden.bs.modal', '#addUserModal', function(){
  var appElement = $('#addUserModal');
  var $scope = angular.element(appElement).scope();
  $scope.$apply(function() {
    for(var e in $scope.coursemodal.sections) {
      $scope.coursemodal.sections[e].selected = false;
    }
    $scope.coursemodal.sectionSelected = false;
    $scope.coursemodal.friendEmailAddress ='';
    $scope.coursemodal.friendNameFirst ='';
    $scope.coursemodal.friendNameLast ='';
    $scope.user = false;
    $scope.newUser = false;
    $scope.newUserFound = false;
    $scope.newUserFail = false;
    $scope.userExists = false;
    $scope.friend = {};
    $scope.addSuccess= false;
    $scope.failedValidation = false;
  });
});;


