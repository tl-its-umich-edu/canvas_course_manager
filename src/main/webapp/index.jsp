<!DOCTYPE html>
<html lang="en" ng-app="sectionsApp">
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
        <link rel="stylesheet" href="assets/vendor/bootstrap/bootstrap.min.css">
        <link rel="stylesheet" href="assets/css/custom_style.css">
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <link rel="stylesheet" href="assets/vendor/jquery/jquery-ui-1.11.2/jquery-ui.min.css">
        <title>Sections Manager</title>
    </head>
    <body ng-controller="coursesController" ng-cloak>
      <div class="alert alert-danger debugPanel" id="debugPanel"></div>
      <nav class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container-fluid">
                <!-- Brand and toggle get grouped for better mobile display -->
                <div class="navbar-header mastHead">
                    <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <h1 class="navbar-brand collapse-nav">Sections Manager <small id="serverInfo">{{server}}</small></h1>
                </div>
                <!-- Collect the nav links, forms, and other content for toggling -->
                <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                    <ul class="nav navbar-nav navbar-right">
                        <li>
                            <a href="#" class="collapse-nav" id="helpLink">Help</a>
                        </li>
                    </ul>
                </div>
                <!-- /.navbar-collapse -->
            </div>
            <!-- /.container-fluid -->
        </nav>
        <div class="alert alert-success reportContainer" id="successContainer" role="alert" style="display:none">
          <div class="msg"></div>
        </div>
        <div class="alert alert-danger alert-dismissible reportContainer" id="errorContainer" role="alert" style="display:none">
          <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <div class="msg"></div>
        </div>

        <div class="container-fluid contentWrapper">
            <div class="btn-group pull-right" ng-show="termArray" ng-controller = "termsController" ng-cloak>
                <button type="button" class="btn btn-sm btn-default dropdown-toggle pull-right" data-toggle="dropdown">
                    Terms <span class="caret"></span>
                </button>
                <ul class="dropdown-menu" role="menu">
                    <li ng-repeat="term in terms | orderBy : sis_term_id" ng-data-id="{{term.id}}" style="display:none">
                        <a href='' ng-click="getTerm(term.sis_term_id, term.name, term.id)">{{term.name}}</a>
                    </li>
                </ul>
            </div>

            <div class="form-inline">

              <div class="form-group">
                <label for="uniqname" class="sr-only">Enter uniqname</label>
                <input type="text"  ng-model="uniqname" id="uniqname" class="form-control"  placeholder="uniqname" autofocus>
              </div>
              <button id="uniqnameTrigger" type="button" ng-click="getCoursesForUniqname()" class="btn btn-primary">Look up courses <span ng-show="uniqname">for</span> {{uniqname}}</button> <span id="uniqnameValidMessage" class="alert alert-danger" style="display:none; padding:5px">Alpha chars only, please.</span>
              <div class="spinner" ng-show="loading">&nbsp;&nbsp;&nbsp;&nbsp;</div>


              
              <span class="alert alert-danger" ng-show="error" role="alert">{{errorMessage}}</span>
              <small ng-show="errorLookup"> Lookup in the 
              <a target="_blank" href="https://mcommunity.umich.edu/#search:{{uniqname}}"> directory</a>?</small>
              <small ng-show="success" class="alert" role="alert">Found {{filtered.length}} courses for <a target="_blank" href="https://mcommunity.umich.edu/#search:{{uniqname}}"> {{uniqname}}</a> for this term.</small><small class="hidden-xs hidden-sm hidden-md" ng-show="instructions">Get all sections, then drag and drop & click on <strong>Update Course</strong>. See <strong>Help</strong> link above for more.</small>
            </div>
            <h4 title="SIS-ID: {{currentTerm.currentTermId}} CV-ID: {{currentTerm.currentTermCanvasId}}"> {{currentTerm.currentTermName}}
            </h4>
            <div class="alert alert-info" ng-show="courses.errors">{{courses.errors}}</div>
            <div class="coursePanel well" ng-show="success && !courses.errors">
                <ul class="container-fluid courseList">
                    <li data-course-id="{{item.id}}" ng-repeat="item in filtered = (courses  | filter:{enrollment_term_id:currentTerm.currentTermCanvasId}) | orderBy:'name'" class="course" ng-class="item.sections.length === 1 ? 'course onlyOneSection' :'course'">
                        <div class="row">
                            <div class="col-md-8">
                                <strong><a class="courseLink" href="{{server}}/courses/{{item.id}}" target="_blank">{{item.course_code}}</a></strong> <span class="courseName" title="CV-TERM-ID: {{item.enrollment_term_id}}" ng-show="item.name">{{item.name}}</span>
                                - <a class="renameCourse" href="">Rename?</a>
                                <span class="form-inline courseTitleTextContainer" style="display:none">
                                  <span class="form-group">
                                    <span class="input-group">
                                      <input type="text" class="form-control courseTitleText input-sm" style="width:200px">
                                    </span>
                                  </span>
                                  <button type="button" class="btn btn-primary btn-sm postCourseNameChange" data-courseid="{{item.id}}">Rename</button>
                                  <button type="button" class="btn btn-link btn-sm cancelCourseNameChange">Cancel</button>
                                </span>

                            </div>
                            <div class="col-md-4">
                                <span class="orphanMessage">Orphan candidate!</span>
                                <div class="btn-group pull-right">
                                    <a href="" class="btn btn-default btn-xs" ng-hide="item.sectionsShown" href="#" ng-click="getSections(item.id)">Get all sections <span class="sr-only"> for {{item.name}}</a> 
                                    <button class="btn btn-default btn-xs getEnrollements" data-courseid="{{item.id}}" data-toggle="modal" data-target="#courseGetEnrollmentsModal" title="Get Enrollments"><span class="sr-only">Get Enrollments</span><i class="glyphicon glyphicon-user"></i></button>
                                    <button class="btn btn-default btn-xs getCourseInfo" data-courseid="{{item.id}}" data-toggle="modal" data-target="#courseInfoModal" title="Get Course Info"><span class="sr-only">Get Course Info</span><i class="glyphicon glyphicon-info-sign"></i></button>
                                    <button ng-show="item.sections.length" class="btn btn-warning btn-xs setSections" data-courseid="{{item.id}}" data-toggle="modal" data-target="#xListModal">Update Course</button>
                                    <button ng-show="item.sectionsShown" class="btn btn-primary btn-xs openOtherInstructorModal" id="openOtherInstructorModal{{item.id}}" data-courseid="{{item.id}}"  data-target="#otherInstructorModal" title="Add other sections"><span class="sr-only">Add other sections</span><i class="glyphicon glyphicon-share-alt"></i></button>

                                </div>
                            </div>
                        </div>
                        <!--//removed  ng-model="item.sections" below //-->
                        <ul ui-sortable="sortableOptions" class="sectionList" id="course{{item.id}}" ng-show="item.sections.length" data-orig-sect-number=
                        {{item.sections.length}}>
                            <li ng-repeat="section in item.sections | orderBy:'name'" class="row section" data-sectionid="{{section.id}}">
                                <div class="col-md-5 sectionName">
                                    <span>{{section.name}}</span>
                                </div>
                                <div class="col-md-7">
                                    <span class="status"></span> <span class="orig" style="display:none"> - Originally from: {{item.course_code}}</span> 
                                    <a href="" ng-show="section.nonxlist_course_id" title="Uncrosslist {{section.name}}"class="pull-right sectionCheck" data-toggle="modal" data-target="#unCrossListModal">
                                      <span class="glyphicon glyphicon-resize-full"></span> <span class="sr-only">Uncrosslist {{section.name}}</a>
                                    </a>
                                </div>        
                            </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
        <script src="assets/vendor/jquery/jquery-1.11.0.min.js"></script>
        <script src="assets/vendor/jquery/jquery-ui-1.11.2/jquery-ui.min.js"></script>
        <script src="assets/vendor/angular/angular.min.js">
        </script>
        <script src="assets/vendor/angular/sortable.js"></script>
        <script src="assets/vendor/bootstrap/bootstrap.min.js">
        </script>
        <script src="assets/vendor/moment/moment.min.js">
        </script>
		<script src="assets/vendor/underscore/underscore-min.js">
        </script>
        <script src="assets/js/utils.js">
        </script>
        <script src="assets/js/controllers/controllers.js">
        </script>
        <script src="assets/js/factories/factories.js">
        </script>
        <script src="assets/js/filters/filters.js">
        </script>

        <!-- XList Modal -->
        <div class="modal fade" id="xListModal" tabindex="-1" role="dialog" aria-labelledby="xListLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">
                            <span aria-hidden="true">&times;</span>
                            <span class="sr-only">Close</span>
                        </button>
                        <h3 class="modal-title" id="xListLabel">Crosslist confirm</h3>
                    </div>
                    <div class="modal-body" id="xListInner">
                    </div>    
                    <div class="modal-footer">
                        <a id="xListConfirm" target="_blank" class="btn btn-link pull-left"  href="" style="display:none">
                            <span id="xListConfirmMessage"></span> Confirm in Canvas?
                        </a>
                        <button type="button" class="btn btn-primary" id="postXList">
                            Crosslist
                        </button>
                        <button type="button" class="btn btn-primary" data-dismiss="modal"  id="postXListDone">
                            Finished?
                        </button> 
                        <button type="button" class="btn btn-link"  data-dismiss="modal" id="postXListCancel">
                            Cancel
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- enrollments Modal -->
        <div class="modal fade" id="courseGetEnrollmentsModal" tabindex="-1" role="dialog" aria-labelledby="courseGetEnrollmentsLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">
                            <span aria-hidden="true">&times;</span>
                            <span class="sr-only">Close</span>
                        </button>
                        <h3 class="modal-title" id="courseGetEnrollmentsLabel"></h3>
                    </div>
                    <div class="modal-body" id="courseGetEnrollmentsInner">
                    </div>    
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary"  data-dismiss="modal">
                            Close
                        </button>
                    </div>
                </div>
            </div>
        </div>



        <!-- courseInfo Modal -->
        <div class="modal fade" id="courseInfoModal" tabindex="-1" role="dialog" aria-labelledby="courseInfoLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">
                            <span aria-hidden="true">&times;</span>
                            <span class="sr-only">Close</span>
                        </button>
                        <h3 class="modal-title" id="courseInfoLabel"></h3>
                    </div>
                    <div class="modal-body" id="courseInfoInner">
                    </div>    
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary"  data-dismiss="modal">
                            Close
                        </button>
                    </div>
                </div>
            </div>
        </div>

      <!-- unCrossList Modal -->
      <div class="modal fade" id="unCrossListModal" tabindex="-1" role="dialog" aria-labelledby="unCrossListLabel" aria-hidden="true">
          <div class="modal-dialog">
              <div class="modal-content">
                  <div class="modal-header">
                      <button type="button" class="close" data-dismiss="modal">
                          <span aria-hidden="true">&times;</span>
                          <span class="sr-only">Close</span>
                      </button>
                      <h3 class="modal-title" id="unCrossListLabel">Uncrosslisting</h3>
                  </div>
                  <div class="modal-body" id="unCrossListInner">
                  </div>
                  <div class="modal-footer">
                      <button id="unCrossList" type="button" class="btn btn-primary" data-section-id="">
                          Uncrosslist
                      </button>
                      <button id="unCrossListDone" type="button" class="btn btn-primary"  data-dismiss="modal" style="display:none">
                          Done
                      </button>

                      <button type="button" class="btn btn-default" data-dismiss="modal">
                          Close
                      </button>
                  </div>
              </div>
          </div>
      </div>

        <!-- otherInstructor Modal -->
        <div class="modal fade" id="otherInstructorModal" tabindex="-1" role="dialog" aria-labelledby="otherInstructorLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">
                            <span aria-hidden="true">&times;</span>
                            <span class="sr-only">Close</span>
                        </button>
                        <h3 class="modal-title" id="otherInstructorLabel">Select sections from other instructors ({{currentTerm.currentTermName}})<span id="canvasTermId" style="display:none">{{currentTerm.currentTermCanvasId}}</span></h3>
                    </div>
                    <div class="modal-body otherInstructorInner" id="otherInstructorInner">
                        <div class="form-inline">
                            <div class="form-group">
                                <label for="uniqnameOther" class="sr-only">Enter uniqname</label>
                                <input type="text"  ng-model="uniqnameOther" id="uniqnameOther" class="form-control"  placeholder="other instructor uniqname" style="width:200px">
                            </div>
                            <button id="uniqnameOtherTrigger" type="button" class="btn btn-primary">Look up courses <span ng-show="uniqnameOther">for</span> {{uniqnameOther}}</button>
                            <div class="otherSpinner" ng-show="loading">&nbsp;&nbsp;&nbsp;&nbsp;</div>
                        </div>
                        <div id="otherInstructorInnerPayload"></div>
                    </div>
                    <div class="modal-footer">
                        <button id="useOtherSections" type="button" class="btn btn-primary">
                            Use selected
                        </button>

                        <button type="button" class="btn btn-link"  data-dismiss="modal">
                            Close
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
